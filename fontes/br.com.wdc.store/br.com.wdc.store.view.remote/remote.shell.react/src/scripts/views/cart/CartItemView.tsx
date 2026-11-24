import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { ActionButton } from "@root/swc"
import * as NumberUtils from "@root/utils/NumberUtils"
import Sel from "./cart-sel"

// :: Actions (must match the row's actions on the server: CartScope.CartItemScope)

const ON_REMOVE = 1
const ON_MODIFY_QUANTITY = 2

// :: View

export type CartItemViewState = {
  id?: number
  name?: string
  price?: number
  quantity?: number
}

/** A row of the cart: a view of its own, whose actions are the row's — no product id goes back. */
class CartItemViewClass extends BaseViewClass<ViewProps, CartItemViewState> {
  override render() {
    const { state } = this
    const price = state.price ?? 0
    const quantity = state.quantity ?? 0
    const subtotal = NumberUtils.formatCurrency(price * quantity)
    return (
      <div className={Sel.itemRow}>
        <span className={Sel.itemName}>{state.name}</span>
        <div className={Sel.stepperRow}>
          <ActionButton quiet size="s" ref={this.decrementBtnRef}>
            <span className={clsx("bi bi-dash", Sel.stepperIcon)}></span>
          </ActionButton>
          <span className={Sel.stepperValue}>{quantity}</span>
          <ActionButton quiet size="s" ref={this.incrementBtnRef}>
            <span className={clsx("bi bi-plus", Sel.stepperIcon)}></span>
          </ActionButton>
        </div>
        <span className={Sel.itemSubtotal}>{subtotal}</span>
        <ActionButton quiet size="s" ref={this.removeBtnRef}>
          <span className={clsx("bi bi-x-lg", Sel.removeIcon)}></span>
        </ActionButton>
      </div>
    )
  }

  // :: Refs

  readonly decrementBtnRef = (el: HTMLElement | null) => {
    if (el) el.onclick = () => this.emitModifyQuantity((this.state.quantity ?? 0) - 1)
  }

  readonly incrementBtnRef = (el: HTMLElement | null) => {
    if (el) el.onclick = () => this.emitModifyQuantity((this.state.quantity ?? 0) + 1)
  }

  readonly removeBtnRef = (el: HTMLElement | null) => {
    if (el) el.onclick = this.emitRemove
  }

  // :: Emissors

  readonly emitModifyQuantity = (quantity: number) => {
    bridge.setFormField(this.vsid, "p.quantity", quantity)
    bridge.submit(this.vsid, ON_MODIFY_QUANTITY)
  }

  readonly emitRemove = () => {
    bridge.submit(this.vsid, ON_REMOVE)
  }
}

export default BaseViewClass.FC(CartItemViewClass, "c8a1f0e2b7d4")
