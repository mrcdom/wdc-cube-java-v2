import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import GlobalSel from '@root/global-sel'

export type RootViewState = {
  contentViewId?: string
  errorMessage?: string
}

class RootViewClass extends BaseViewClass<ViewProps, RootViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this

    if (state.errorMessage) {
      return (
        <div className={GlobalSel.alertError} style={{ margin: '16px' }}>
          <span className={GlobalSel.alertErrorIcon}>
            <i className="bi bi-exclamation-circle"></i>
          </span>
          <span className={GlobalSel.alertErrorText}>{state.errorMessage}</span>
        </div>
      )
    }

    if (state.contentViewId) {
      return bridge.createView(state.contentViewId)
    }

    return (
      <div className={GlobalSel.alertError} style={{ margin: '16px' }}>
        <span className={GlobalSel.alertErrorIcon}>
          <i className="bi bi-exclamation-circle"></i>
        </span>
        <span className={GlobalSel.alertErrorText}>Falta conteúdo para a página inicial</span>
      </div>
    )
  }
}

export default BaseViewClass.FC(RootViewClass, 'f2d345c4a610')
