import type { ViewStateCoordinator } from "./ViewStateCoordinator"
import type { BrowserViewState, FormMapType } from "./types"
import { BROWSER_VSID, KEEP_ALIVE_INTERVAL } from "./constants"

export class FlushRequestContext {
  private readonly app: ViewStateCoordinator
  private readonly onVisibilityChange = () => {
    if (!document.hidden && this.socket?.readyState === WebSocket.OPEN) {
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
  private pendingSecret: string | null = null
  pendingStorage: Record<string, unknown> | null = null

  constructor(app: ViewStateCoordinator) {
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

    if (socket?.readyState !== WebSocket.OPEN) {
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

    if (hasData || this.pendingSecret || this.pendingStorage) {
      if (this.pendingSecret) {
        requestObj.secret = this.pendingSecret
        this.pendingSecret = null
      }
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

  open(url: string) {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    const app = this.app

    const socket = (this.socket = new WebSocket(url, ["wdc"]))
    ;(socket as Record<string, unknown>).withCredentials = true

    const handleDisconnect = (cause: unknown) => {
      this.socket = null
      app.isConnected = false
      this.stopKeepAliveChecks()
      this.userRequestIds.clear()
      this.stopSubmitting()
      app.reconnectController.reconnect(cause)
    }

    socket.onopen = () => {
      app.isConnected = true
      this.pendingSecret = app.dataSecurity.getSignature()
      this.initKeepAliveChecks()
      // Build bootstrap storage payload (async: cipher key may have just been set)
      // then flush. Nothing is sent until the promise resolves.
      app
        .buildBootstrapStorage()
        .then((storage) => {
          this.pendingStorage = storage
          this.flush()
        })
        .catch(() => {
          this.flush() // send even if storage build fails
        })
    }

    socket.onerror = (error) => {
      handleDisconnect(error)
    }

    socket.onclose = (event: CloseEvent) => {
      // Server sent close code 4001: session is invalid, reload the page
      if (event.code === 4001) {
        globalThis.location.reload()
        return
      }
      handleDisconnect(event)
    }

    // Log messages from the server
    socket.onmessage = (e) => {
      if (app.reconnectController.count > 0) {
        app.reconnectController.reset()
      }
      const response = JSON.parse(e.data)

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

      if (response.states) {
        app.applyViewStates(response.states)
      }

      if (response.storage) {
        app.applyStorageDelta(response.storage as Record<string, unknown>).catch(CAUTHED)
      }

      this.flush()
      if (this.userRequestIds.size > 0) {
        this.startSubmitting()
      } else {
        this.stopSubmitting()
      }
    }
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
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.pendingKeepAlive = globalThis.setTimeout(() => {
        this.pendingKeepAlive = 0
        this.persistState()
        this.socket?.send(JSON.stringify({ ping: true }))
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
    const scope = this.app.viewMap.get(BROWSER_VSID)
    if (scope) {
      const state = scope.getState() as BrowserViewState
      if (state.submitting !== value) {
        state.submitting = value
        scope.forceUpdate()
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
