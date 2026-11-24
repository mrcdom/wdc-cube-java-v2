import React from "react"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import GlobalSel from "@root/global-sel"

export type RootScope = {
  contentViewId?: string
  errorMessage?: string
}

class RootViewClass extends BaseViewClass<ViewProps, RootScope> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { scope } = this

    if (scope.errorMessage) {
      return (
        <div className={GlobalSel.alertError} style={{ margin: "16px" }}>
          <span className={GlobalSel.alertErrorIcon}>
            <i className="bi bi-exclamation-circle"></i>
          </span>
          <span className={GlobalSel.alertErrorText}>{scope.errorMessage}</span>
        </div>
      )
    }

    if (scope.contentViewId) {
      return bridge.createView(scope.contentViewId)
    }

    return (
      <div className={GlobalSel.alertError} style={{ margin: "16px" }}>
        <span className={GlobalSel.alertErrorIcon}>
          <i className="bi bi-exclamation-circle"></i>
        </span>
        <span className={GlobalSel.alertErrorText}>Missing content for the home page</span>
      </div>
    )
  }
}

export default BaseViewClass.FC(RootViewClass, "f2d345c4a610")
