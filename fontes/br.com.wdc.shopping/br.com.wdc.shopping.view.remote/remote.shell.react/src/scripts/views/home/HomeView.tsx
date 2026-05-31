import React from 'react'
import clsx from 'clsx'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import HeaderPanel from './HeaderPanel'
import Sel from './home-sel'

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
      <div className={clsx(Sel.flexCol, Sel.flexGrow, Sel.flex1, Sel.minH0, Sel.overflowHidden)}>
        <HeaderPanel vsid={vsid} nickName={state.nickName} cartItemCount={state.cartItemCount} />
        {/* Error */}
        {state.errorMessage ? (
          <div className={Sel.alertError}>
            <span className={clsx('bi bi-exclamation-circle', Sel.alertErrorIcon)}></span>
            <span className={Sel.alertErrorText}>{state.errorMessage}</span>
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
        <div className={clsx(Sel.flexCol, Sel.flexGrow, Sel.overflowAuto, Sel.minH0, Sel.bgDefault)}>
          {bridge.createView(state.contentViewId)}
        </div>
      )
    }

    // Default split: products + purchases with tab nav
    const productsHide = this.showingProducts ? '' : Sel.mdShow
    const purchasesHide = this.showingProducts ? Sel.mdShow : ''

    return (
      <div className={clsx(Sel.flexCol, Sel.flexGrow, Sel.minH0, Sel.overflowHidden)}>
        {/* Tab navigation (mobile only) */}
        <nav className={clsx(Sel.mdHide, Sel.tabNav)}>
          <button
            className={clsx(Sel.tabItem, this.showingProducts ? Sel.tabItemActive : Sel.tabItemInactive)}
            onClick={this.onTabProducts}
          >
            <span className="bi bi-grid-3x3-gap text-base"></span>
            <span>Produtos</span>
            {this.showingProducts ? <span className={Sel.tabIndicator}></span> : <span className={Sel.hidden}></span>}
          </button>
          <button
            className={clsx(Sel.tabItem, !this.showingProducts ? Sel.tabItemActive : Sel.tabItemInactive)}
            onClick={this.onTabHistory}
          >
            <span className="bi bi-clock-history text-base"></span>
            <span>Histórico</span>
            {!this.showingProducts ? <span className={Sel.tabIndicator}></span> : <span className={Sel.hidden}></span>}
          </button>
        </nav>
        {/* Split row: products + purchases */}
        <div className={clsx(Sel.mdRow, Sel.flex, Sel.flexGrow, Sel.overflowAuto, Sel.minH0, Sel.bgDefault)}>
          <div className={clsx(productsHide, Sel.flexCol, Sel.flexGrow, Sel.hFull)}>
            {bridge.createView(state.productsPanelViewId)}
          </div>
          <div className={clsx(Sel.purchasesSlot, Sel.mdGrow0, purchasesHide, Sel.flexCol, Sel.flexGrow, Sel.hFull)}>
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
