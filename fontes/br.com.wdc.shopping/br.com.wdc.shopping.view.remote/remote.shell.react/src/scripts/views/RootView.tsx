import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'

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
        <div className="alert-error" style={{ margin: '16px' }}>
          <span className="alert-error-icon">
            <i className="bi bi-exclamation-circle"></i>
          </span>
          <span className="alert-error-text">{state.errorMessage}</span>
        </div>
      )
    }

    if (state.contentViewId) {
      return bridge.createView(state.contentViewId)
    }

    return (
      <div className="alert-error" style={{ margin: '16px' }}>
        <span className="alert-error-icon">
          <i className="bi bi-exclamation-circle"></i>
        </span>
        <span className="alert-error-text">Falta conteúdo para a página inicial</span>
      </div>
    )
  }
}

export default BaseViewClass.FC(RootViewClass, 'f2d345c4a610')
