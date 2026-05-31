import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import { Button, ActionButton } from '@root/swc'

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
      <div className={`page-scroll-root ${className || ''}`}>
        <div className="page-wrapper">
          <div className="card-panel">
            {/* Header */}
            <div className="card-header-row">
              <div className="card-header-icon-box">
                <span className="bi bi-bag card-header-icon"></span>
              </div>
              <div>
                <h5 className="card-header-title">Carrinho</h5>
                <span className="card-header-subtitle">Seus produtos selecionados</span>
              </div>
            </div>

            {/* Error */}
            {state.errorMessage && (
              <div className="alert-error mb-16">
                <span className="bi bi-exclamation-circle alert-error-icon"></span>
                <span className="alert-error-text">{state.errorMessage}</span>
              </div>
            )}

            {/* Empty state */}
            {empty ? (
              <div className="empty-state py-48">
                <div className="cart-empty-icon-box">
                  <span className="bi bi-bag cart-empty-icon"></span>
                </div>
                <p className="cart-empty-title">Carrinho vazio</p>
                <p className="cart-empty-subtitle">Adicione produtos para começar</p>
                <Button variant="accent" ref={this.viewProductsBtnRef}>
                  <span className="bi bi-grid-3x3-gap mr-6"></span>
                  <span>Ver produtos</span>
                </Button>
              </div>
            ) : (
              <div>
                {/* Items list */}
                <div>{items.map((item) => this.renderItem(item))}</div>

                {/* Footer total */}
                <div className="cart-footer">
                  <span className="cart-footer-label">Total: </span>
                  <span className="cart-footer-total">{totalText}</span>
                </div>

                {/* Actions */}
                <div className="cart-actions-row">
                  <ActionButton quiet ref={this.backBtnRef}>
                    <span className="bi bi-arrow-left"></span>
                    <span> Continuar comprando</span>
                  </ActionButton>
                  <Button variant="accent" size="l" ref={this.buyBtnRef}>
                    <span className="bi bi-check2-circle mr-6"></span>
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
      <div key={item.id} className="cart-item-row">
        <span className="cart-item-name">{item.name}</span>
        <div className="cart-stepper-row">
          <ActionButton
            quiet
            size="s"
            ref={(el: HTMLElement | null) => {
              if (el) el.onclick = () => this.emitModifyQuantity(item.id, item.quantity - 1)
            }}
          >
            <span className="bi bi-dash cart-stepper-icon"></span>
          </ActionButton>
          <span className="cart-stepper-value">{item.quantity}</span>
          <ActionButton
            quiet
            size="s"
            ref={(el: HTMLElement | null) => {
              if (el) el.onclick = () => this.emitModifyQuantity(item.id, item.quantity + 1)
            }}
          >
            <span className="bi bi-plus cart-stepper-icon"></span>
          </ActionButton>
        </div>
        <span className="cart-item-subtotal">{subtotal}</span>
        <ActionButton
          quiet
          size="s"
          ref={(el: HTMLElement | null) => {
            if (el) el.onclick = () => this.emitRemove(item.id)
          }}
        >
          <span className="bi bi-x-lg cart-remove-icon"></span>
        </ActionButton>
      </div>
    )
  }

  // :: Refs

  readonly backBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitBack)
  }

  readonly buyBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitBuy)
  }

  readonly viewProductsBtnRef = (el: HTMLElement | null) => {
    if (el) el.addEventListener('click', this.emitBack)
  }

  // :: Emissors

  readonly emitBack = () => {
    bridge.submit(this.vsid, ON_BACK)
  }

  readonly emitBuy = () => {
    bridge.submit(this.vsid, ON_BUY)
  }

  readonly emitModifyQuantity = (id: number, quantity: number) => {
    bridge.setFormField(this.vsid, 'p.productId', id)
    bridge.setFormField(this.vsid, 'p.quantity', quantity)
    bridge.submit(this.vsid, ON_MODIFY_QUANTITY)
  }

  readonly emitRemove = (id: number) => {
    bridge.setFormField(this.vsid, 'p.productId', id)
    bridge.submit(this.vsid, ON_REMOVE)
  }
}

export default BaseViewClass.FC(CartViewClass, '7eb485e5f843')
