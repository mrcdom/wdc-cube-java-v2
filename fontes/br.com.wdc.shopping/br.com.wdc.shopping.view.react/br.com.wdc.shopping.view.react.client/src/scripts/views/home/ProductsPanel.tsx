import React, { ReactNode } from 'react'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardContent from '@mui/material/CardContent'
import CardMedia from '@mui/material/CardMedia'
import Chip from '@mui/material/Chip'
import Grid from '@mui/material/Grid'
import Typography from '@mui/material/Typography'
import app, { type ViewProps } from '@root/App'
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
      <Grid className={className} container spacing={2} sx={{ flex: 1 }}>
        {divProdutos.map((card, idx) => (
          <Grid key={idx} size={{ xs: 12, sm: 6, md: 4 }}>
            {card}
          </Grid>
        ))}
      </Grid>
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
      <Card elevation={2} sx={{ height: '100%' }}>
        <CardActionArea
          onClick={this.emitOpenProduct}
          sx={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'stretch' }}
        >
          <CardMedia
            component="img"
            image={EndpointUtils.productImagePath(product.id)}
            alt={product.name}
            sx={{ width: '100%', height: 160, objectFit: 'contain', p: 1, bgcolor: '#fafafa' }}
          />
          <CardContent sx={{ bgcolor: '#e5e5e5', flexGrow: 1 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              {product.name}
            </Typography>
            <Chip label={`R$ ${NumberUtils.format(product.price)}`} size="small" color="primary" />
          </CardContent>
        </CardActionArea>
      </Card>
    )
  }

  // :: Emissors

  emitOpenProduct = () => {
    const { vsid, product } = this
    app.setFormField(vsid, 'p.productId', product.id)
    app.submit(vsid, ON_OPEN_PRODUCT)
  }
}

const CardProduto = BasePanelClass.FC(CardProdutoClass)
