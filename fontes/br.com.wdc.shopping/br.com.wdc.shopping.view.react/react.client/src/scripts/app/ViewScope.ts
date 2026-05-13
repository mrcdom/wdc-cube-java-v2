import * as LangUtils from '../utils/LangUtils'
import { NOOP_VOID } from './constants'

export class ViewScope {
  private readonly __svid: string
  private readonly __viewState: Record<string, unknown> = {}
  private __referencedViewIds: Set<string> = new Set()

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

  getReferencedViewIds() {
    return this.__referencedViewIds
  }

  setState(newViewState: Record<string, unknown>) {
    const newRefs = ViewScope.extractViewIdRefs(newViewState)
    this.__referencedViewIds = newRefs

    LangUtils.deleteProperties(this.__viewState)
    Object.assign(this.__viewState, newViewState)
    this.forceUpdate()
  }

  private static extractViewIdRefs(state: Record<string, unknown>): Set<string> {
    const refs = new Set<string>()
    for (const key in state) {
      if (key.endsWith('ViewId')) {
        const value = state[key]
        if (typeof value === 'string' && value.length > 0) {
          refs.add(value)
        }
      }
    }
    return refs
  }
}
