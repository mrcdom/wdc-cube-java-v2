import type { ScopeCoordinator } from "./ScopeCoordinator"
import type { BrowserScope, FormMapType } from "./types"
import { BROWSER_VSID, CAUTHED, KEEP_ALIVE_INTERVAL } from "./constants"
import { PROTOCOL } from "./ChannelCrypto"

export class FlushRequestContext {
  private readonly app: ScopeCoordinator
  private readonly onVisibilityChange = () => {
    if (!document.hidden && this.isReady()) {
      this.keepAliveNow()
    }
  }

  socket: WebSocket | null
  requestMap = new Map<number, FormMapType>()
  lastSentRequestId = -1
  requestCount = 0
  lastProcessedId = -1
  keepAliveHandler = 0
  pendingKeepAlive = 0
  private submittingTimer = 0
  private submittingTimeout = 0
  private readonly userRequestIds = new Set<number>()
  /** The connection is open and waits for the host's answer to its handshake: nothing else may go out. */
  private handshaking = false
  /** The first message after the handshake answer (the bootstrap) has not gone out yet. */
  private bootstrapPending = false
  /** The host refused this bridge's protocol version: no message goes out, no reconnection is tried. */
  outdated = false
  pendingStorage: Record<string, unknown> | null = null
  /** Incoming messages, handled one at a time, in order (the handshake answer is asynchronous). */
  private inbox: Promise<void> = Promise.resolve()

  constructor(app: ScopeCoordinator) {
    this.app = app
    this.socket = null

    // Restore request counter from sessionStorage (survives F5)
    const savedSeq = sessionStorage.getItem("req_seq")
    if (savedSeq) {
      const parsed = Number.parseInt(savedSeq, 10)
      if (!Number.isNaN(parsed) && parsed > 0) {
        this.requestCount = parsed
      }
    }

    document.addEventListener("visibilitychange", this.onVisibilityChange)
  }

  submit(formMap: FormMapType, vsid: string, eventId: number, silent = false) {
    this.cancelPendingKeepAlive()

    formMap.requestId = this.requestCount++
    if (formMap.event) {
      formMap.event.push(vsid + ":" + eventId)
    } else {
      formMap.event = [vsid + ":" + eventId]
    }
    this.requestMap.set(formMap.requestId, formMap)

    if (!silent) {
      this.userRequestIds.add(formMap.requestId)
    }

    this.persistState()
    this.resetKeepAliveTimer()
    this.flush()
  }

  flush() {
    // NOSONAR: complexity is inherent to the WebSocket protocol handling
    const { socket, lastSentRequestId, requestCount, requestMap } = this

    if (!socket || !this.isReady()) {
      return
    }

    const requestObj: Record<string, unknown> = { event: [] }
    let hasData = false

    for (let i = lastSentRequestId + 1; i < requestCount; i++) {
      const requestItemObj = requestMap.get(i)
      if (!requestItemObj) {
        continue
      }

      for (const key of Object.keys(requestItemObj)) {
        const value = requestItemObj[key]
        if (value) {
          if (key === "event") {
            for (const item of value as string[]) {
              ;(requestObj.event as string[]).push(item)
            }
          } else {
            let formData = requestObj[key] as object
            if (!formData) {
              formData = {}
              requestObj[key] = formData
            }
            Object.assign(formData, value)
          }
        }
      }
      requestObj.requestId = i
      this.lastSentRequestId = i
      hasData = true
    }

    if (hasData || this.bootstrapPending) {
      this.bootstrapPending = false
      if (this.pendingStorage) {
        requestObj.storage = this.pendingStorage
        this.pendingStorage = null
      }
      socket.send(JSON.stringify(requestObj))
      if (this.userRequestIds.size > 0) {
        this.startSubmitting()
      }
    }
  }

  /** The connection is open and its handshake is done. */
  private isReady() {
    return this.socket?.readyState === WebSocket.OPEN && !this.handshaking && !this.outdated
  }

  open(url: string) {
    if (this.outdated) {
      return
    }
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    const app = this.app

    const socket = (this.socket = new WebSocket(url, ["wdc"]))
    ;(socket as unknown as Record<string, unknown>).withCredentials = true

    const handleDisconnect = (cause: unknown) => {
      if (this.socket !== socket) {
        return
      }
      this.socket = null
      this.handshaking = false
      app.isConnected = false
      this.stopKeepAliveChecks()
      this.userRequestIds.clear()
      this.stopSubmitting()
      if (!this.outdated) {
        app.reconnectController.reconnect(cause)
      }
    }

    socket.onopen = () => {
      app.isConnected = true
      this.handshaking = true
      this.bootstrapPending = false
      this.pendingStorage = null
      app.dataSecurity
        .beginHandshake()
        .then((hello) => {
          if (this.socket === socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(hello))
          }
        })
        .catch((error) => {
          CAUTHED(error)
          socket.close()
        })
    }

    socket.onerror = (error) => {
      handleDisconnect(error)
    }

    socket.onclose = (event: CloseEvent) => {
      if (event.code === 4001) {
        // Session rejected by the server (reload_required): discard the stored
        // app_id before reloading — otherwise the reload reconnects with the same
        // invalid id and enters an infinite reload loop.
        sessionStorage.removeItem("app_id")
        globalThis.location.reload()
        return
      }
      if (event.code === 4002) {
        this.markOutdated()
      }
      handleDisconnect(event)
    }

    socket.onmessage = (e) => {
      const data = e.data as string
      this.inbox = this.inbox
        .then(() => (this.socket === socket ? this.handleMessage(socket, data) : undefined))
        .catch(CAUTHED)
    }
  }

  /** The answer to the handshake: the connection's key, then the bootstrap; or the refusal of the version. */
  private async handleHandshakeAnswer(socket: WebSocket, answer: Record<string, unknown>) {
    if (answer.error === "unsupported-protocol") {
      this.markOutdated()
      return
    }
    if (answer.protocol !== PROTOCOL || typeof answer.serverKey !== "string") {
      throw new Error("Unexpected handshake answer: " + JSON.stringify(answer))
    }

    const app = this.app
    await app.dataSecurity.completeHandshake(answer.serverKey, app.id)

    let storage: Record<string, unknown> | null = null
    try {
      storage = await app.buildBootstrapStorage()
    } catch (error) {
      CAUTHED(error) // the bootstrap goes out without the storage
    }
    if (this.socket !== socket) {
      return
    }
    this.handshaking = false
    this.bootstrapPending = true
    this.pendingStorage = storage
    this.initKeepAliveChecks()
    this.flush()
  }

  private async handleMessage(socket: WebSocket, data: string) {
    const app = this.app
    const response = JSON.parse(data)

    if (this.handshaking) {
      try {
        await this.handleHandshakeAnswer(socket, response)
      } catch (error) {
        CAUTHED(error)
        socket.close()
      }
      return
    }

    if (app.reconnectController.count > 0) {
      app.reconnectController.reset()
    }

    if (response.releasedViews) {
      app.viewGarbageCollector.release(response.releasedViews)
    }
    if (response.activeViews) {
      app.viewGarbageCollector.sweep(response.activeViews)
    }

    if (response.requestId != null) {
      for (let i = this.lastProcessedId + 1; i <= response.requestId; i++) {
        this.requestMap.delete(i)
        this.userRequestIds.delete(i)
        this.lastProcessedId = i
      }
    }

    if (response.uri) {
      app.path = response.uri
      const hashValue = `#${response.uri}`
      if (app.firstUriResponse || app.navigatingFromPopState) {
        app.firstUriResponse = false
        app.navigatingFromPopState = false
        globalThis.history.replaceState(null, "", hashValue)
      } else {
        globalThis.history.pushState(null, "", hashValue)
      }
    }

    if (response.scopes) {
      app.applyScopes(response.scopes)
    }

    if (response.storage) {
      await app.applyStorageDelta(response.storage as Record<string, unknown>)
    }

    this.flush()
    if (this.userRequestIds.size > 0) {
      this.startSubmitting()
    } else {
      this.stopSubmitting()
    }
  }

  /** The host refused this bridge's protocol version: stop, never reconnect, and tell the user. */
  private markOutdated() {
    if (this.outdated) {
      return
    }
    this.outdated = true
    this.handshaking = false
    this.stopKeepAliveChecks()
    this.cancelPendingKeepAlive()
    this.userRequestIds.clear()
    this.stopSubmitting()
    this.app.reconnectController.close()
    this.app.showOutdated()
  }

  close() {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
  }

  private initKeepAliveChecks() {
    this.stopKeepAliveChecks()
    this.keepAliveHandler = globalThis.setTimeout(this.keepAlive, KEEP_ALIVE_INTERVAL)
  }

  private stopKeepAliveChecks() {
    globalThis.clearTimeout(this.keepAliveHandler)
    this.keepAliveHandler = 0
  }

  private keepAliveNow() {
    this.cancelPendingKeepAlive()
    if (this.isReady()) {
      this.pendingKeepAlive = globalThis.setTimeout(() => {
        this.pendingKeepAlive = 0
        this.persistState()
        if (this.isReady()) {
          this.socket?.send(JSON.stringify({ ping: true }))
        }
      }, 80)
    }
  }

  private cancelPendingKeepAlive() {
    if (this.pendingKeepAlive !== 0) {
      globalThis.clearTimeout(this.pendingKeepAlive)
      this.pendingKeepAlive = 0
    }
  }

  private resetKeepAliveTimer() {
    if (this.keepAliveHandler !== 0) {
      this.stopKeepAliveChecks()
      this.keepAliveHandler = globalThis.setTimeout(this.keepAlive, KEEP_ALIVE_INTERVAL)
    }
  }

  private startSubmitting() {
    if (this.submittingTimer === 0) {
      this.submittingTimer = globalThis.setTimeout(() => {
        this.submittingTimer = 0
        this.applySubmitting(true)
      }, 200)
    }
    if (this.submittingTimeout === 0) {
      this.submittingTimeout = globalThis.setTimeout(() => {
        this.submittingTimeout = 0
        this.stopSubmitting()
      }, 15_000)
    }
  }

  private stopSubmitting() {
    if (this.submittingTimer !== 0) {
      globalThis.clearTimeout(this.submittingTimer)
      this.submittingTimer = 0
    }
    if (this.submittingTimeout !== 0) {
      globalThis.clearTimeout(this.submittingTimeout)
      this.submittingTimeout = 0
    }
    this.applySubmitting(false)
  }

  private applySubmitting(value: boolean) {
    const view = this.app.viewMap.get(BROWSER_VSID)
    if (view) {
      const scope = view.getValues() as BrowserScope
      if (scope.submitting !== value) {
        scope.submitting = value
        view.forceUpdate()
      }
    }
  }

  private persistState() {
    sessionStorage.setItem("req_seq", String(this.requestCount))
  }

  private readonly keepAlive = () => {
    this.stopKeepAliveChecks()
    this.keepAliveNow()
    this.keepAliveHandler = globalThis.setTimeout(this.keepAlive, KEEP_ALIVE_INTERVAL)
  }
}
