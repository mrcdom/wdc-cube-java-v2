import React from "react"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import GlobalSel from "@root/global-sel"
import { type Message, t, tm } from "@root/utils/I18n"

export type RootScope = {
  contentViewId?: string
  errorMessage?: Message
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
          <span className={GlobalSel.alertErrorText}>{tm(scope.errorMessage)}</span>
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
        <span className={GlobalSel.alertErrorText}>{t("home.missingContent")}</span>
      </div>
    )
  }
}

export default BaseViewClass.FC(RootViewClass, "f2d345c4a610")
