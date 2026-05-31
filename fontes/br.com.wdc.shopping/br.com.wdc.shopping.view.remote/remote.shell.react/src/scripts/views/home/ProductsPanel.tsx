import React, { ReactNode } from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass, BasePanelClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as EndpointUtils from '@root/utils/EndpointUtils'

// :: Actions

const ON_OPEN_PRODUCT = 1

// :: Types

export type Product = {
  id: number
  name: string
  price: number
}

type ProductsPanelState = {
  products?: Product[]
}

// :: View

class ProductPanelClass extends BaseViewClass<ViewProps, ProductsPanelState> {
  override render({ className }: ViewProps): React.ReactNode {
    const { vsid, state } = this

    const divProdutos: ReactNode[] = []
    if (state.products) {
      for (let i = 0; i < state.products.length; i++) {
        let produto = state.products[i]
        divProdutos.push(<CardProduto key={produto.id} vsid={vsid} product={produto} />)
      }
    }

    return (
      <div className={`products-panel ${className || ''}`}>
        <div className="product-grid">{divProdutos}</div>
      </div>
    )
  }
}
export default BaseViewClass.FC(ProductPanelClass, 'a1b2c3d4e5f6')

// :: Internal - CardProduto

type CardProdutoProps = {
  vsid: string
  product: Product
}

class CardProdutoClass extends BasePanelClass<CardProdutoProps> {
  vsid!: string
  product!: Product

  override render({ vsid, product }: CardProdutoProps): React.ReactNode {
    this.vsid = vsid
    this.product = product

    return (
      <div className="product-card" onClick={this.emitOpenProduct}>
        <div className="products-card-image-wrap">
          <img className="products-card-image" src={EndpointUtils.productImagePath(product.id)} alt={product.name} />
        </div>
        <div className="products-card-body">
          <p className="products-card-name">{product.name}</p>
          <span className="products-card-price">R$ {NumberUtils.format(product.price)}</span>
        </div>
      </div>
    )
  }

  // :: Emissors

  emitOpenProduct = () => {
    const { vsid, product } = this
    bridge.setFormField(vsid, 'p.productId', product.id)
    bridge.submit(vsid, ON_OPEN_PRODUCT)
  }
}

const CardProduto = BasePanelClass.FC(CardProdutoClass)
