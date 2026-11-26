import React from "react"
import { type BridgeOptions, ScopeCoordinator } from "./ScopeCoordinator"
import { BROWSER_VSID } from "./constants"
import type { IViewFactory, ViewComponent } from "./types"

export { BROWSER_VID, OUTDATED_MESSAGE } from "./constants"
export type { BridgeOptions } from "./ScopeCoordinator"
export type { BrowserScope, ViewComponent, ViewProps } from "./types"
export { ViewScope } from "./ViewScope"

let privateApp: ScopeCoordinator

const publicApp = new (class {
  configure(syncNamespace: string, options?: BridgeOptions) {
    privateApp = new ScopeCoordinator(syncNamespace, options)
  }

  getBaseUrl() {
    return privateApp.baseWebSocketUtl
  }

  getScope<T>(vsid: string) {
    const viewScope = privateApp.viewMap.get(vsid)
    return viewScope ? (viewScope.getValues() as T) : ({} as T)
  }

  connect() {
    privateApp.connect()
  }

  submit(vsid: string, eventId: number) {
    privateApp.submit(vsid, eventId)
  }

  submitSilent(vsid: string, eventId: number) {
    privateApp.submitSilent(vsid, eventId)
  }

  /** Ciphers a sensitive value with the connection key; waits for the connection's handshake if needed. */
  async cipher(value: string) {
    return privateApp.dataSecurity.b64Cipher(value)
  }

  setFormField(vsid: string, fieldName: string, fieldValue: unknown) {
    privateApp.setFormField(vsid, fieldName, fieldValue)
  }

  registerView(viewId: string, factory: IViewFactory) {
    privateApp.registerView(viewId, factory)
  }

  registerComponent(Component: ViewComponent) {
    privateApp.registerView(Component.VIEW_ID, (vsid, props) => <Component vsid={vsid} {...props} />)
  }

  registerComponents(...components: ViewComponent[]) {
    for (const Component of components) {
      this.registerComponent(Component)
    }
  }

  createView(vsid: string | null | undefined, props?: Record<string, unknown>) {
    if (!vsid) {
      return null
    }
    return privateApp.createView(vsid, props)
  }

  createBrowserView() {
    return privateApp.createView(BROWSER_VSID)
  }

  bindView<T>(vsid: string) {
    return privateApp.bindView<T>(vsid)
  }

  onStart() {
    privateApp.onStart()
  }

  onStop() {
    privateApp.onStop()
  }
})()

export default publicApp
