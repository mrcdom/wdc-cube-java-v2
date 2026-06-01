import { ViewStateCoordinator } from './ViewStateCoordinator'
import { BROWSER_VSID } from './constants'
import type { IViewFactory, ViewComponent } from './types'

export { BROWSER_VID } from './constants'
export type { BrowserViewState, ViewComponent, ViewProps } from './types'
export { ViewScope } from './ViewScope'

async function static_updateAllViewStates(app: ViewStateCoordinator, vsids: string[]) {
  const url = `view-state`

  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      'X-Application-Id': app.id,
    },
    body: JSON.stringify(vsids),
  })

  const viewStates = (await resp.json()) as { id: string }[]
  app.applyViewStates(viewStates)
}

const privateApp = new ViewStateCoordinator()

const publicApp = new (class {
  getBaseUrl() {
    return privateApp.baseWebSocketUtl
  }

  getViewState<T>(vsid: string) {
    const viewScope = privateApp.viewMap.get(vsid)
    return viewScope ? (viewScope.getState() as T) : ({} as T)
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

  updateViewState(vsid: string) {
    static_updateAllViewStates(privateApp, [vsid])
  }

  onStart() {
    privateApp.onStart()
  }

  onStop() {
    privateApp.onStop()
  }
})()

export default publicApp
