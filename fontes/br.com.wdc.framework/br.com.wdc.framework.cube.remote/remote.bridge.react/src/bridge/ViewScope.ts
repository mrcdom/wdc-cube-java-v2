import * as LangUtils from '../utils/LangUtils'
import { NOOP_VOID } from './constants'

export class ViewScope {
  private readonly __svid: string
  private readonly __viewState: Record<string, unknown> = {}

  forceUpdate = NOOP_VOID

  constructor(svid: string) {
    this.__svid = svid
  }

  getId() {
    return this.__svid
  }

  getState() {
    return this.__viewState
  }

  setState(newViewState: Record<string, unknown>) {
    LangUtils.deleteProperties(this.__viewState)
    Object.assign(this.__viewState, newViewState)
    this.forceUpdate()
  }
}
