import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import HeaderPanel from './home/HeaderPanel'

// :: View

export type HomeViewState = {
  nickName: string
  cartItemCount: number
  productsPanelViewId?: string
  purchasesPanelViewId?: string
  contentViewId?: string
  errorMessage: string
}

class HomeViewClass extends BaseViewClass<ViewProps, HomeViewState> {
  private showingProducts = true

  // :: Renderes

  override render({ vsid }: ViewProps) {
    const { state } = this

    return (
      <div className="flex-col flex-grow flex-1 min-h-0 overflow-hidden">
        <HeaderPanel vsid={vsid} nickName={state.nickName} cartItemCount={state.cartItemCount} />
        {/* Error */}
        {state.errorMessage ? (
          <div className="alert-error">
            <span className="bi bi-exclamation-circle alert-error-icon"></span>
            <span className="alert-error-text">{state.errorMessage}</span>
          </div>
        ) : null}
        {this.renderContentPane()}
      </div>
    )
  }

  private renderContentPane() {
    const { state } = this

    // When a content child (product detail, cart, receipt) is showing
    if (state.contentViewId) {
      return (
        <div className="flex-col flex-grow overflow-auto min-h-0 bg-default">
          {bridge.createView(state.contentViewId)}
        </div>
      )
    }

    // Default split: products + purchases with tab nav
    const productsHide = this.showingProducts ? '' : 'md-show'
    const purchasesHide = this.showingProducts ? 'md-show' : ''

    return (
      <div className="flex-col flex-grow min-h-0 overflow-hidden">
        {/* Tab navigation (mobile only) */}
        <nav className="md-hide tab-nav">
          <button
            className={this.showingProducts ? 'tab-item tab-item--active' : 'tab-item tab-item--inactive'}
            onClick={this.onTabProducts}
          >
            <span className="bi bi-grid-3x3-gap text-base"></span>
            <span>Produtos</span>
            {this.showingProducts ? <span className="tab-indicator"></span> : <span className="hidden"></span>}
          </button>
          <button
            className={!this.showingProducts ? 'tab-item tab-item--active' : 'tab-item tab-item--inactive'}
            onClick={this.onTabHistory}
          >
            <span className="bi bi-clock-history text-base"></span>
            <span>Histórico</span>
            {!this.showingProducts ? <span className="tab-indicator"></span> : <span className="hidden"></span>}
          </button>
        </nav>
        {/* Split row: products + purchases */}
        <div className="md-row flex flex-grow overflow-auto min-h-0 bg-default">
          <div className={`${productsHide} flex-col flex-grow h-full`}>
            {bridge.createView(state.productsPanelViewId)}
          </div>
          <div className={`slot-purchases md-grow-0 ${purchasesHide} flex-col flex-grow h-full`}>
            {bridge.createView(state.purchasesPanelViewId)}
          </div>
        </div>
      </div>
    )
  }

  // :: Event Handlers

  readonly onTabProducts = () => {
    this.switchTab(true)
  }

  readonly onTabHistory = () => {
    this.switchTab(false)
  }

  private switchTab(showProducts: boolean) {
    const { state } = this
    if (state.contentViewId) {
      window.history.back()
    }
    this.showingProducts = showProducts
    this.forceUpdate()
  }
}

export default BaseViewClass.FC(HomeViewClass, '473dbdd7a36a')
