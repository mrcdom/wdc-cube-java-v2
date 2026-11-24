import * as LangUtils from "../utils/LangUtils"
import { NOOP_VOID } from "./constants"

export class ViewScope {
  private readonly __svid: string
  private readonly __values: Record<string, unknown> = {}

  forceUpdate = NOOP_VOID

  constructor(svid: string) {
    this.__svid = svid
  }

  getId() {
    return this.__svid
  }

  getValues() {
    return this.__values
  }

  setValues(values: Record<string, unknown>) {
    LangUtils.deleteProperties(this.__values)
    Object.assign(this.__values, values)
    this.forceUpdate()
  }
}
