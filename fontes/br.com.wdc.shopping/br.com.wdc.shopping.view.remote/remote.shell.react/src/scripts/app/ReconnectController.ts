import type { Application } from './Application'
import type { BrowserViewState } from './types'
import { BROWSER_VSID } from './constants'

export class ReconnectController {
  app: Application
  url = ''
  count = 0
  reconnectHandler = 0
  delay = 0
  cause: unknown

  constructor(app: Application) {
    this.app = app
    this.url = app.getBaseWebSocketUrl() + '/dispatcher/' + app.id
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
      const bvScope = app.viewMap.get(BROWSER_VSID)
      if (bvScope) {
        const bvState = bvScope.getState() as BrowserViewState
        bvState.error = {
          cause: this.cause,
          numAttempt: this.count,
          delay: this.delay,
        }
        bvScope.forceUpdate()
      }
    }

    if (this.reconnectHandler === 0) {
      this.reconnectHandler = window.setInterval(() => this.check(), 1000)
    }
  }

  check() {
    const app = this.app
    const bvScope = app.viewMap.get(BROWSER_VSID)
    if (!bvScope) {
      this.reset()
      return
    }

    if (app.isConnected) {
      this.reset()
      return
    }

    const bvState = bvScope.getState() as BrowserViewState
    bvState.error = {
      cause: this.cause,
      numAttempt: this.count,
      delay: this.delay,
    }

    if (this.delay > 0) {
      this.delay -= 1000
      this.delay < 0 && (this.delay = 0)
      bvState.error.delay = this.delay
    }

    bvScope.forceUpdate()

    if (this.delay <= 0) {
      window.setTimeout(app.assureContextExchangerIsConnected, 16)
    }
  }

  reset() {
    const app = this.app
    const browserView = app.viewMap.get(BROWSER_VSID)
    if (browserView) {
      const browserViewState = browserView.getState()
      browserViewState.error = undefined
      browserView.setState(browserViewState)
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
