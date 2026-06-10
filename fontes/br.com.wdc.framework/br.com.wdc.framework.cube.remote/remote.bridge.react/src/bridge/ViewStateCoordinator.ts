import React from 'react'
import * as LangUtils from '../utils/LangUtils'
import CookieConstructor from 'universal-cookie'

import type { FormMapType, IViewFactory } from './types'
import { NOOP_PROMISE_VOID, CAUTHED, BROWSER_VSID } from './constants'
import { ViewScope } from './ViewScope'
import { DataSecurity } from './DataSecurity'
import { FlushRequestContext } from './FlushRequestContext'
import { ReconnectController } from './ReconnectController'
import { ViewGarbageCollector } from './ViewGarbageCollector'
import { ClientStorage, InMemoryClientStorage, LocalStorageClientStorage } from './ClientStorage'

const Cookie = new CookieConstructor()

export class ViewStateCoordinator {
  readonly id: string

  /** Session-scoped storage: in-memory, lives while the tab is open. */
  readonly sessionStorage: ClientStorage
  /** Persistent-scoped storage: backed by localStorage, survives page reload. */
  readonly persistentStorage: ClientStorage

  readonly viewFactory = new Map<string, IViewFactory>()
  readonly viewMap = new Map<string, ViewScope>()

  formMap: FormMapType = {}

  isConnected = false
  path = ''
  baseWebSocketUtl = ''
  private popStateHandler: ((event: PopStateEvent) => void) | null = null
  navigatingFromPopState = false
  firstUriResponse = true

  readonly dataSecurity = new DataSecurity()
  readonly contextExchanger: FlushRequestContext
  readonly reconnectController: ReconnectController
  readonly viewGarbageCollector: ViewGarbageCollector

  readyToStart = NOOP_PROMISE_VOID

  constructor() {
    this.viewMap.set(BROWSER_VSID, new ViewScope(BROWSER_VSID))

    // Storage: session is in-memory; persistent is localStorage-backed.
    // .secure uses 'sec.' key prefix to namespace sensitive values.
    const secureStorage = new LocalStorageClientStorage('sec.', [], () => new InMemoryClientStorage())
    this.sessionStorage = new InMemoryClientStorage()
    this.persistentStorage = new LocalStorageClientStorage('', ['app_', 'sec.', 'req_seq'], () => secureStorage)

    const appIdFromCookie = Cookie.get('app_id')
    if (appIdFromCookie) {
      Cookie.remove('app_id', { path: '/' })
    }

    let appId = sessionStorage.getItem('app_id')
    if (!appId) {
      appId = appIdFromCookie
      if (appId) {
        sessionStorage.setItem('app_id', appId)
      } else {
        appId = LangUtils.makeUniqueId() + '.fake'
      }
    }

    this.id = appId

    const appSKey = Cookie.get('app_skey')
    if (appSKey) {
      this.dataSecurity.updateSecurityKey(appSKey)
      Cookie.remove('app_skey', { path: '/' })

      this.readyToStart = async () => {
        try {
          await this.dataSecurity.updateSecretWithRandomPassword()

          Cookie.set('app_signature', this.dataSecurity.getSignature(), { path: '/' })
          this.readyToStart = NOOP_PROMISE_VOID
        } catch (error) {
          CAUTHED(error)
        }
      }
    }

    const contextPath = location.pathname.split('/')[1]

    this.baseWebSocketUtl =
      (document.location.protocol === 'http:' ? 'ws://' : 'wss://') +
      document.location.host +
      '/' +
      (contextPath ?? 'unknown')

    const slashPos = this.baseWebSocketUtl.indexOf('/', 10)
    if (slashPos != -1) {
      this.baseWebSocketUtl = this.baseWebSocketUtl.substring(0, slashPos)
    }

    this.contextExchanger = new FlushRequestContext(this)
    this.reconnectController = new ReconnectController(this)
    this.viewGarbageCollector = new ViewGarbageCollector(this)
  }

  getBaseWebSocketUrl() {
    return this.baseWebSocketUtl
  }

  registerView(viewId: string, factory: IViewFactory) {
    this.viewFactory.set(viewId, factory)
  }

  createView(vsid: string, props?: Record<string, unknown>) {
    const parts = vsid.split(/:/g)
    const viewCreator = this.viewFactory.get(parts[0])
    if (!viewCreator) {
      throw new Error(`Nenhuma view registrada para a viewId: "${parts[0]}"`)
    }

    let viewScope = this.viewMap.get(vsid)
    if (!viewScope) {
      viewScope = new ViewScope(vsid)
      this.viewMap.set(vsid, viewScope)
    }

    return viewCreator(vsid, props ?? {})
  }

  connect() {
    this.reconnectController.checkNow()
  }

  bindView<T>(vsid: string) {
    let scope = this.viewMap.get(vsid)
    if (!scope) {
      scope = new ViewScope(vsid)
      this.viewMap.set(vsid, scope)
    }

    const [, setUpdateCount] = React.useState(0) // NOSONAR: bindView is called from function components only
    scope.forceUpdate = () => setUpdateCount((count) => count + 1)

    React.useEffect(() => { // NOSONAR: bindView is called from function components only
      this.viewGarbageCollector.mount(vsid)
      return () => {
        if (vsid !== BROWSER_VSID) {
          this.viewGarbageCollector.unmount(vsid)
        }
      }
    }, [])

    return { state: scope.getState() as T, scope }
  }

  onStart() {
    const action = async () => {
      // Wait construction async initialization
      await this.readyToStart()

      this.assureContextExchangerIsConnected()

      // Listen for browser back/forward using raw popstate
      // (pushState/replaceState do NOT fire popstate — no loop)
      if (this.popStateHandler) {
        globalThis.removeEventListener('popstate', this.popStateHandler)
      }
      this.popStateHandler = () => {
        const hash = globalThis.location.hash
        const newPath = hash && hash.length > 1 ? hash.substring(1) : '/'
        if (this.path !== newPath) {
          this.navigatingFromPopState = true
          this.onHistoryChange(newPath)
        }
      }
      globalThis.addEventListener('popstate', this.popStateHandler)

      const hash = globalThis.location.hash
      this.path = hash && hash.length > 1 ? hash.substring(1) : '/'

      this.setFormField(BROWSER_VSID, 'p.path', this.path)
      this.submit(BROWSER_VSID, -1)
    }

    action().catch(CAUTHED)
  }

  onStop() {
    if (this.popStateHandler) {
      globalThis.removeEventListener('popstate', this.popStateHandler)
      this.popStateHandler = null
    }
  }

  onHistoryChange(path: string) {
    this.setFormField(BROWSER_VSID, 'p.path', path)
    this.submit(BROWSER_VSID, -2)
  }

  applyViewStates(stateList: { '#': string }[]) {
    for (const viewState of stateList) {
      if (!viewState?.['#']) {
        continue
      }
      const vsid = viewState['#']

      let viewScope = this.viewMap.get(vsid)
      if (!viewScope) {
        viewScope = new ViewScope(vsid)
        this.viewMap.set(vsid, viewScope)
      }

      viewScope.setState(viewState)
    }
  }

  submit(vsid: string, eventId: number) {
    const oldFormMap = this.formMap
    this.formMap = {}
    const silent = vsid === BROWSER_VSID
    this.contextExchanger.submit(oldFormMap, vsid, eventId, silent)
  }

  submitSilent(vsid: string, eventId: number) {
    const oldFormMap = this.formMap
    this.formMap = {}
    this.contextExchanger.submit(oldFormMap, vsid, eventId, true)
  }

  setFormField(vsid: string, fieldName: string, fieldValue: unknown) {
    let formData = this.formMap[vsid] as Record<string, unknown>
    if (!formData) {
      formData = {}
      this.formMap[vsid] = formData
    }
    formData[fieldName] = fieldValue
  }

  readonly assureContextExchangerIsConnected = () => {
    this.contextExchanger.open(this.reconnectController.url)
  }

  /**
   * Builds the bootstrap `storage` map sent on WebSocket open.
   * All values are ciphered. Returns `null` if the cipher key is not ready.
   */
  async buildBootstrapStorage(): Promise<Record<string, unknown> | null> {
    if (!this.dataSecurity.getSignature()) return null

    const result: Record<string, unknown> = {}

    const addScope = async (scopeName: string, storage: ClientStorage) => {
      const entries = storage.all()
      const keys = Object.keys(entries)
      if (keys.length === 0) return
      const scoped: Record<string, string> = {}
      for (const key of keys) {
        scoped[key] = await this.dataSecurity.b64Cipher(entries[key])
      }
      result[scopeName] = scoped
    }

    await addScope('session', this.sessionStorage)
    await addScope('persistent', this.persistentStorage)
    await addScope('persistent-secure', this.persistentStorage.secure)

    return Object.keys(result).length > 0 ? result : null
  }

  /**
   * Applies a storage delta received from the server.
   * Values are deciphered; a `null`/empty value means removal.
   */
  async applyStorageDelta(delta: Record<string, unknown>): Promise<void> {
    if (!this.dataSecurity.getSignature()) return

    const applyScope = async (scope: Record<string, string> | undefined, storage: ClientStorage) => {
      if (!scope) return
      for (const [key, ciphered] of Object.entries(scope)) {
        if (!ciphered) {
          storage.remove(key)
        } else {
          storage.set(key, await this.dataSecurity.b64Decipher(ciphered))
        }
      }
    }

    await applyScope(delta['session'] as Record<string, string>, this.sessionStorage)
    await applyScope(delta['persistent'] as Record<string, string>, this.persistentStorage)
    await applyScope(delta['persistent-secure'] as Record<string, string>, this.persistentStorage.secure)
  }
}
