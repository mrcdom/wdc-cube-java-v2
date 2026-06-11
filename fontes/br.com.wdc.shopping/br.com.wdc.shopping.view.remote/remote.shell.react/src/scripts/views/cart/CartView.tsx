import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { Button, ActionButton } from "@root/swc"
import Sel from "./cart-sel"

// :: Actions (must match server-side presenter)

const ON_BUY = 1
const ON_REMOVE = 2
const ON_BACK = 3
const ON_MODIFY_QUANTITY = 4

// :: Types

type CartItem = {
  id: number
  name: string
  price: number
  quantity: number
}

// :: View

export type CartViewState = {
  items?: CartItem[]
  errorMessage?: string
}

class CartViewClass extends BaseViewClass<ViewProps, CartViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this
    const items = state.items ?? []
    const empty = items.length === 0
    const total = items.reduce((sum, item) => sum + item.price * item.quantity, 0)
    const totalText = `R$ ${total.toFixed(2)}`

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
                <h5 className={Sel.cardHeaderTitle}>Carrinho</h5>
                <span className={Sel.cardHeaderSubtitle}>Seus produtos selecionados</span>
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
                <p className={Sel.emptyTitle}>Carrinho vazio</p>
                <p className={Sel.emptySubtitle}>Adicione produtos para começar</p>
                <Button variant="accent" ref={this.viewProductsBtnRef}>
                  <span className={clsx("bi bi-grid-3x3-gap", Sel.mr6)}></span>
                  <span>Ver produtos</span>
                </Button>
              </div>
            ) : (
              <div>
                {/* Items list */}
                <div>{items.map((item) => this.renderItem(item))}</div>

                {/* Footer total */}
                <div className={Sel.footer}>
                  <span className={Sel.footerLabel}>Total: </span>
                  <span className={Sel.footerTotal}>{totalText}</span>
                </div>

                {/* Actions */}
                <div className={Sel.actionsRow}>
                  <ActionButton quiet ref={this.backBtnRef}>
                    <span className="bi bi-arrow-left"></span>
                    <span> Continuar comprando</span>
                  </ActionButton>
                  <Button variant="accent" size="l" ref={this.buyBtnRef}>
                    <span className={clsx("bi bi-check2-circle", Sel.mr6)}></span>
                    <span>Finalizar pedido</span>
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    )
  }

  private renderItem(item: CartItem) {
    const subtotal = `R$ ${(item.price * item.quantity).toFixed(2)}`
    return (
      <div key={item.id} className={Sel.itemRow}>
        <span className={Sel.itemName}>{item.name}</span>
        <div className={Sel.stepperRow}>
          <ActionButton
            quiet
            size="s"
            ref={(el: HTMLElement | null) => {
              if (el) el.onclick = () => this.emitModifyQuantity(item.id, item.quantity - 1)
            }}
          >
            <span className={clsx("bi bi-dash", Sel.stepperIcon)}></span>
          </ActionButton>
          <span className={Sel.stepperValue}>{item.quantity}</span>
          <ActionButton
            quiet
            size="s"
            ref={(el: HTMLElement | null) => {
              if (el) el.onclick = () => this.emitModifyQuantity(item.id, item.quantity + 1)
            }}
          >
            <span className={clsx("bi bi-plus", Sel.stepperIcon)}></span>
          </ActionButton>
        </div>
        <span className={Sel.itemSubtotal}>{subtotal}</span>
        <ActionButton
          quiet
          size="s"
          ref={(el: HTMLElement | null) => {
            if (el) el.onclick = () => this.emitRemove(item.id)
          }}
        >
          <span className={clsx("bi bi-x-lg", Sel.removeIcon)}></span>
        </ActionButton>
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

  readonly emitModifyQuantity = (id: number, quantity: number) => {
    bridge.setFormField(this.vsid, "p.productId", id)
    bridge.setFormField(this.vsid, "p.quantity", quantity)
    bridge.submit(this.vsid, ON_MODIFY_QUANTITY)
  }

  readonly emitRemove = (id: number) => {
    bridge.setFormField(this.vsid, "p.productId", id)
    bridge.submit(this.vsid, ON_REMOVE)
  }
}

export default BaseViewClass.FC(CartViewClass, "7eb485e5f843")
