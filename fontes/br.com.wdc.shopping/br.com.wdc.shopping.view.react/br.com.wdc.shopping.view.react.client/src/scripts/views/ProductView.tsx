import React from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass } from '@root/utils/ViewUtils'
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
      <Card elevation={3} sx={{ maxWidth: 900, mx: 'auto', my: 3 }}>
        <CardContent>
          {/* Breadcrumb */}
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            <strong>Produtos &gt; {product.name}</strong>
          </Typography>

          <Divider sx={{ mb: 3 }} />

          {/* Product body */}
          <Box sx={{ display: 'flex', gap: 3, flexWrap: 'wrap' }}>
            {/* Image */}
            <Box
              component="img"
              src={EndpointUtils.productImagePath(product.id)}
              alt={product.name}
              sx={{
                width: 300,
                height: 300,
                objectFit: 'contain',
                border: '1px solid',
                borderColor: 'grey.400',
                borderRadius: 1,
                flexShrink: 0,
              }}
            />

            {/* Details */}
            <Box sx={{ flex: 1, minWidth: 220 }}>
              <Typography variant="h5" gutterBottom>
                {product.name}
              </Typography>
              <Typography variant="h6" color="primary" gutterBottom>
                R$ {NumberUtils.format(product.price)}
              </Typography>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, my: 2 }}>
                <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                  Quantidade:
                </Typography>
                <TextField
                  type="number"
                  name="quantidade"
                  value={quantity}
                  onChange={this.emitQtaChanged}
                  size="small"
                  slotProps={{ htmlInput: { min: 1, style: { width: 60 } } }}
                />
              </Box>

              <Divider sx={{ my: 2 }} />

              <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 'bold' }}>
                DESCRIÇÃO DO PRODUTO
              </Typography>
              <Box
                sx={{ fontSize: 14, color: 'text.secondary', mb: 2 }}
                dangerouslySetInnerHTML={{ __html: product.description }}
              />

              {state.errorMessage && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {state.errorMessage}
                </Alert>
              )}

              <Button
                variant="contained"
                color="warning"
                size="large"
                startIcon={<AddShoppingCartIcon />}
                onClick={this.emitAddToCart}
              >
                Adicionar ao carrinho
              </Button>
            </Box>
          </Box>

          {/* Navigation */}
          <Box sx={{ mt: 3 }}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={this.emitGoHome}>
              Voltar
            </Button>
          </Box>
        </CardContent>
      </Card>
    )
  }

  // :: Emissors

  readonly emitAddToCart = () => {
    const { vsid, state } = this
    app.setFormField(vsid, 'p.quantity', state.quantity)
    app.submit(vsid, ON_ADD_TO_CART)
  }

  readonly emitGoHome = () => {
    const { vsid } = this
    app.submit(vsid, ON_OPEN_PRODUCTS)
  }

  readonly emitQtaChanged = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { vsid, state } = this
    state.quantity = Number.parseInt(e.target.value.trim() || '0') | 0
    app.setFormField(vsid, 'quantity', state.quantity)
    this.forceUpdate()
  }
}

export default BaseViewClass.FC(ProductViewClass, '48b693f67410')
