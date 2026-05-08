import React, { ReactNode } from 'react'
import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardActionArea from '@mui/material/CardActionArea'
import CardMedia from '@mui/material/CardMedia'
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
      <Grid className={className} container spacing={3} sx={{ flex: 1 }}>
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
      <Card
        elevation={0}
        sx={{
          height: '100%',
          border: '1px solid',
          borderColor: 'grey.200',
          borderRadius: 3,
          overflow: 'hidden',
          transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 12px 24px rgba(0,0,0,0.1)',
            borderColor: 'primary.light',
          },
        }}
      >
        <CardActionArea
          onClick={this.emitOpenProduct}
          sx={{ height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'stretch' }}
        >
          <Box
            sx={{
              position: 'relative',
              bgcolor: '#f8f9fa',
              borderBottom: '1px solid',
              borderColor: 'grey.100',
            }}
          >
            <CardMedia
              component="img"
              image={EndpointUtils.productImagePath(product.id)}
              alt={product.name}
              sx={{ width: '100%', height: 180, objectFit: 'contain', p: 2 }}
            />
          </Box>
          <Box sx={{ p: 2, flexGrow: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <Typography
              variant="body2"
              sx={{
                color: 'text.primary',
                fontWeight: 500,
                mb: 1.5,
                lineHeight: 1.4,
                minHeight: '2.8em',
              }}
            >
              {product.name}
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Typography
                variant="body1"
                sx={{
                  color: 'primary.main',
                  fontWeight: 'bold',
                  fontSize: '1.1rem',
                }}
              >
                R$ {NumberUtils.format(product.price)}
              </Typography>
            </Box>
          </Box>
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
