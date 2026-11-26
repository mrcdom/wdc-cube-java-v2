import type { ScopeCoordinator } from "./ScopeCoordinator"
import type { BrowserScope } from "./types"
import { BROWSER_VSID } from "./constants"

export class ReconnectController {
  app: ScopeCoordinator
  url = ""
  count = 0
  reconnectHandler = 0
  delay = 0
  cause: unknown

  constructor(app: ScopeCoordinator) {
    this.app = app
    this.url = app.getBaseWebSocketUrl() + "/dispatcher/" + app.id
  }

  close() {
    clearInterval(this.reconnectHandler)
    this.reconnectHandler = 0
  }

  reconnect(cause: unknown) {
    const app = this.app

    this.count++
    this.delay = Math.min(2000 * this.count, 120000)
    this.cause = cause

    {
      const bvView = app.viewMap.get(BROWSER_VSID)
      if (bvView) {
        const bvScope = bvView.getValues() as BrowserScope
        bvScope.error = {
          cause: this.cause,
          numAttempt: this.count,
          delay: this.delay,
        }
        bvView.forceUpdate()
      }
    }

    if (this.reconnectHandler === 0) {
      this.reconnectHandler = globalThis.setInterval(() => this.check(), 1000)
    }
  }

  check() {
    const app = this.app
    if (app.contextExchanger.outdated) {
      this.close()
      return
    }
    const bvView = app.viewMap.get(BROWSER_VSID)
    if (!bvView) {
      this.reset()
      return
    }

    if (app.isConnected) {
      this.reset()
      return
    }

    const bvScope = bvView.getValues() as BrowserScope
    bvScope.error = {
      cause: this.cause,
      numAttempt: this.count,
      delay: this.delay,
    }

    if (this.delay > 0) {
      this.delay -= 1000
      this.delay < 0 && (this.delay = 0)
      bvScope.error.delay = this.delay
    }

    bvView.forceUpdate()

    if (this.delay <= 0) {
      globalThis.setTimeout(app.assureContextExchangerIsConnected, 16)
    }
  }

  reset() {
    const app = this.app
    const browserView = app.viewMap.get(BROWSER_VSID)
    if (browserView) {
      const browserScope = browserView.getValues()
      browserScope.error = undefined
      browserView.setValues(browserScope)
    }

    this.count = 0
    this.delay = 0
    this.cause = null

    clearInterval(this.reconnectHandler)
    this.reconnectHandler = 0
  }

  checkNow() {
    this.delay = 0
    this.check()
  }
}
