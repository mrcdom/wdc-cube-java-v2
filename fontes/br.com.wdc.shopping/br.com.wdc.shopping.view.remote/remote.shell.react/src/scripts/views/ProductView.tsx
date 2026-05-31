import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import { Button, ActionButton, Divider } from '@root/swc'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as EndpointUtils from '@root/utils/EndpointUtils'

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
  name: '',
  description: '',
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
      <div className={`page-scroll-root ${className || ''}`}>
        <div className="page-wrapper">
          {/* Product title */}
          <h5 className="product-title">{product.name}</h5>
          <Divider size="s" className="product-divider"></Divider>

          {/* Description card */}
          <div className="product-desc-card">
            <div className="product-desc-text" dangerouslySetInnerHTML={{ __html: product.description }} />
          </div>

          {/* Price + Image row */}
          <div className="product-price-image-row">
            <div className="product-price-col">
              <span className="product-price-badge">R$ {NumberUtils.format(product.price)}</span>
              <div className="product-qty-row">
                <span className="product-qty-label">Qtd:</span>
                <ActionButton quiet size="s" onClick={this.emitDecrement}>
                  <i className="bi bi-dash"></i>
                </ActionButton>
                <span className="product-qty-value">{quantity}</span>
                <ActionButton quiet size="s" onClick={this.emitIncrement}>
                  <i className="bi bi-plus"></i>
                </ActionButton>
              </div>
            </div>
            <div className="product-image-box">
              <img className="product-image" src={EndpointUtils.productImagePath(product.id)} alt={product.name} />
            </div>
          </div>

          {/* Actions */}
          <div className="product-actions-row">
            <ActionButton quiet onClick={this.emitGoHome}>
              <i className="bi bi-arrow-left mr-4"></i>
              Voltar
            </ActionButton>
            <Button variant="accent" size="l" onClick={this.emitAddToCart}>
              <i className="bi bi-bag-plus mr-4"></i>
              Adicionar ao Carrinho
            </Button>
          </div>

          {/* Error */}
          {state.errorMessage && (
            <div className="alert-error mt-16">
              <span className="alert-error-icon">
                <i className="bi bi-exclamation-circle"></i>
              </span>
              <span className="alert-error-text">{state.errorMessage}</span>
            </div>
          )}
        </div>
      </div>
    )
  }

  // :: Emissors

  readonly emitAddToCart = () => {
    const { vsid, state } = this
    bridge.setFormField(vsid, 'p.quantity', state.quantity)
    bridge.submit(vsid, ON_ADD_TO_CART)
  }

  readonly emitGoHome = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_OPEN_PRODUCTS)
  }

  readonly emitIncrement = () => {
    const { vsid, state } = this
    state.quantity = (state.quantity ?? 1) + 1
    bridge.setFormField(vsid, 'quantity', state.quantity)
    this.forceUpdate()
  }

  readonly emitDecrement = () => {
    const { vsid, state } = this
    const cur = state.quantity ?? 1
    if (cur > 1) {
      state.quantity = cur - 1
      bridge.setFormField(vsid, 'quantity', state.quantity)
      this.forceUpdate()
    }
  }
}

export default BaseViewClass.FC(ProductViewClass, '48b693f67410')
