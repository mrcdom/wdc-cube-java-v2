import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { Button, ActionButton, Divider } from "@root/swc"
import * as NumberUtils from "@root/utils/NumberUtils"
import * as EndpointUtils from "@root/utils/EndpointUtils"
import Sel from "./product-sel"

// :: Actions

const ON_OPEN_PRODUCTS = 1
const ON_ADD_TO_CART = 2

// :: Types

type Product = {
  id: number
  name: string
  description: string
  price: number
}

const DefaultProduct: Product = {
  id: -1,
  name: "",
  description: "",
  price: 0,
}

// :: View

export type ProductViewState = {
  product: Product
  quantity: number
  errorMessage?: string
}

class ProductViewClass extends BaseViewClass<ViewProps, ProductViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this
    const quantity = (state.quantity = state.quantity ?? 1)
    const product = state.product ?? DefaultProduct

    return (
      <div className={clsx(Sel.pageScrollRoot, className)}>
        <div className={Sel.pageWrapper}>
          {/* Product title */}
          <h5 className={Sel.title}>{product.name}</h5>
          <Divider size="s" className={Sel.divider}></Divider>

          {/* Description card */}
          <div className={Sel.descCard}>
            <div className={Sel.descText} dangerouslySetInnerHTML={{ __html: product.description }} />
          </div>

          {/* Price + Image row */}
          <div className={Sel.priceImageRow}>
            <div className={Sel.priceCol}>
              <span className={Sel.priceBadge}>R$ {NumberUtils.format(product.price)}</span>
              <div className={Sel.qtyRow}>
                <span className={Sel.qtyLabel}>Qtd:</span>
                <ActionButton quiet size="s" onClick={this.emitDecrement}>
                  <i className="bi bi-dash"></i>
                </ActionButton>
                <span className={Sel.qtyValue}>{quantity}</span>
                <ActionButton quiet size="s" onClick={this.emitIncrement}>
                  <i className="bi bi-plus"></i>
                </ActionButton>
              </div>
            </div>
            <div className={Sel.imageBox}>
              <img className={Sel.image} src={EndpointUtils.productImagePath(product.id)} alt={product.name} />
            </div>
          </div>

          {/* Actions */}
          <div className={Sel.actionsRow}>
            <ActionButton quiet onClick={this.emitGoHome}>
              <i className={clsx("bi bi-arrow-left", Sel.mr4)}></i>
              Voltar
            </ActionButton>
            <Button variant="accent" size="l" onClick={this.emitAddToCart}>
              <i className={clsx("bi bi-bag-plus", Sel.mr4)}></i>
              Adicionar ao Carrinho
            </Button>
          </div>

          {/* Error */}
          {state.errorMessage && (
            <div className={clsx(Sel.alertError, Sel.mt16)}>
              <span className={Sel.alertErrorIcon}>
                <i className="bi bi-exclamation-circle"></i>
              </span>
              <span className={Sel.alertErrorText}>{state.errorMessage}</span>
            </div>
          )}
        </div>
      </div>
    )
  }

  // :: Emissors

  readonly emitAddToCart = () => {
    const { vsid, state } = this
    bridge.setFormField(vsid, "p.quantity", state.quantity)
    bridge.submit(vsid, ON_ADD_TO_CART)
  }

  readonly emitGoHome = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_OPEN_PRODUCTS)
  }

  readonly emitIncrement = () => {
    const { vsid, state } = this
    state.quantity = (state.quantity ?? 1) + 1
    bridge.setFormField(vsid, "quantity", state.quantity)
    this.forceUpdate()
  }

  readonly emitDecrement = () => {
    const { vsid, state } = this
    const cur = state.quantity ?? 1
    if (cur > 1) {
      state.quantity = cur - 1
      bridge.setFormField(vsid, "quantity", state.quantity)
      this.forceUpdate()
    }
  }
}

export default BaseViewClass.FC(ProductViewClass, "48b693f67410")
