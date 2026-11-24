import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { Button, ActionButton } from "@root/swc"
import * as NumberUtils from "@root/utils/NumberUtils"
import Sel from "./cart-sel"

// :: Actions (must match server-side presenter)

const ON_BUY = 1
const ON_BACK = 3

// :: View

export type CartViewState = {
  /** The rows: each one a view of its own (CartItemView), in the cart's order. */
  rowsViewIds?: string[]
  total?: number
  errorMessage?: string
}

class CartViewClass extends BaseViewClass<ViewProps, CartViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this
    const rows = state.rowsViewIds ?? []
    const empty = rows.length === 0
    const totalText = NumberUtils.formatCurrency(state.total ?? 0)

    return (
      <div className={clsx(Sel.pageScrollRoot, className)}>
        <div className={Sel.pageWrapper}>
          <div className={Sel.cardPanel}>
            {/* Header */}
            <div className={Sel.cardHeaderRow}>
              <div className={Sel.cardHeaderIconBox}>
                <span className={clsx("bi bi-bag", Sel.cardHeaderIcon)}></span>
              </div>
              <div>
                <h5 className={Sel.cardHeaderTitle}>Cart</h5>
                <span className={Sel.cardHeaderSubtitle}>Your selected products</span>
              </div>
            </div>

            {/* Error */}
            {state.errorMessage && (
              <div className={clsx(Sel.alertError, Sel.mb16)}>
                <span className={clsx("bi bi-exclamation-circle", Sel.alertErrorIcon)}></span>
                <span className={Sel.alertErrorText}>{state.errorMessage}</span>
              </div>
            )}

            {/* Empty state */}
            {empty ? (
              <div className={clsx(Sel.emptyState, Sel.py24)}>
                <div className={Sel.emptyIconBox}>
                  <span className={clsx("bi bi-bag", Sel.emptyIcon)}></span>
                </div>
                <p className={Sel.emptyTitle}>Your cart is empty</p>
                <p className={Sel.emptySubtitle}>Add products to get started</p>
                <Button variant="accent" ref={this.viewProductsBtnRef}>
                  <span className={clsx("bi bi-grid-3x3-gap", Sel.mr6)}></span>
                  <span>View products</span>
                </Button>
              </div>
            ) : (
              <div>
                {/* Items list */}
                <div>
                  {rows.map((rowId) => (
                    <React.Fragment key={rowId}>{bridge.createView(rowId)}</React.Fragment>
                  ))}
                </div>

                {/* Footer total */}
                <div className={Sel.footer}>
                  <span className={Sel.footerLabel}>Total: </span>
                  <span className={Sel.footerTotal}>{totalText}</span>
                </div>

                {/* Actions */}
                <div className={Sel.actionsRow}>
                  <ActionButton quiet ref={this.backBtnRef}>
                    <span className="bi bi-arrow-left"></span>
                    <span> Continue shopping</span>
                  </ActionButton>
                  <Button variant="accent" size="l" ref={this.buyBtnRef}>
                    <span className={clsx("bi bi-check2-circle", Sel.mr6)}></span>
                    <span>Checkout</span>
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  // :: Refs

  readonly backBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener("click", this.emitBack)
  }

  readonly buyBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener("click", this.emitBuy)
  }

  readonly viewProductsBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener("click", this.emitBack)
  }

  // :: Emissors

  readonly emitBack = () => {
    bridge.submit(this.vsid, ON_BACK)
  }

  readonly emitBuy = () => {
    bridge.submit(this.vsid, ON_BUY)
  }
}

export default BaseViewClass.FC(CartViewClass, "7eb485e5f843")
