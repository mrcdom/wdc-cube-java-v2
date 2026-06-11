import React, { ReactNode } from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass, BasePanelClass } from "@root/utils/ViewUtils"
import * as NumberUtils from "@root/utils/NumberUtils"
import * as EndpointUtils from "@root/utils/EndpointUtils"
import Sel from "./home-sel"

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
      <div className={clsx(Sel.productsPanel, className)}>
        <div className={Sel.productGrid}>{divProdutos}</div>
      </div>
    )
  }
}
export default BaseViewClass.FC(ProductPanelClass, "a1b2c3d4e5f6")

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
      <div className={Sel.productCard} onClick={this.emitOpenProduct}>
        <div className={Sel.productCardImage}>
          <img className={Sel.productCardImg} src={EndpointUtils.productImagePath(product.id)} alt={product.name} />
        </div>
        <div className={Sel.productCardBody}>
          <p className={Sel.productCardName}>{product.name}</p>
          <span className={Sel.productCardPrice}>R$ {NumberUtils.format(product.price)}</span>
        </div>
      </div>
    )
  }

  // :: Emissors

  emitOpenProduct = () => {
    const { vsid, product } = this
    bridge.setFormField(vsid, "p.productId", product.id)
    bridge.submit(vsid, ON_OPEN_PRODUCT)
  }
}

const CardProduto = BasePanelClass.FC(CardProdutoClass)
