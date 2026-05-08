import React from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import IconButton from '@mui/material/IconButton'
import Link from '@mui/material/Link'
import Typography from '@mui/material/Typography'
import RemoveIcon from '@mui/icons-material/Remove'
import AddIcon from '@mui/icons-material/Add'
import Snackbar from '@mui/material/Snackbar'
import MuiAlert from '@mui/material/Alert'
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
      <Box
        sx={{
          maxWidth: 900,
          mx: 'auto',
          bgcolor: '#fff',
          borderRadius: '12px',
          border: '1px solid #e0e0e0',
          p: 3,
        }}
      >
        {/* Product name */}
        <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 1 }}>
          {product.name}
        </Typography>

        {/* Row: info panel (left) + image (right) */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, mt: 1 }}>
          {/* Left: price + quantity + button */}
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'center' }}>
            <Typography
              sx={{
                fontSize: '1.75rem',
                fontWeight: 'bold',
                color: '#1976d2',
                mt: 0.5,
              }}
            >
              R$ {NumberUtils.format(product.price)}
            </Typography>

            <Typography variant="caption" sx={{ color: 'text.secondary', mt: 1 }}>
              Quantidade
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 0.5 }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  border: '1px solid',
                  borderColor: 'grey.400',
                  borderRadius: 1,
                  overflow: 'hidden',
                }}
              >
                <IconButton size="small" onClick={this.emitDecrement} disabled={quantity <= 1}>
                  <RemoveIcon fontSize="small" />
                </IconButton>
                <Typography
                  sx={{
                    minWidth: 40,
                    textAlign: 'center',
                    fontSize: '0.875rem',
                    userSelect: 'none',
                    px: 1,
                  }}
                >
                  {quantity}
                </Typography>
                <IconButton size="small" onClick={this.emitIncrement}>
                  <AddIcon fontSize="small" />
                </IconButton>
              </Box>

              <Button
                variant="contained"
                color="primary"
                startIcon={<AddShoppingCartIcon />}
                onClick={this.emitAddToCart}
              >
                Adicionar
              </Button>
            </Box>
          </Box>

          {/* Right: product image */}
          <Box
            component="img"
            src={EndpointUtils.productImagePath(product.id)}
            alt={product.name}
            sx={{
              width: 240,
              height: 240,
              objectFit: 'contain',
              flexShrink: 0,
              p: 1,
            }}
          />
        </Box>

        {/* Description label */}
        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
            fontWeight: 600,
            mt: 1,
          }}
        >
          Descrição
        </Typography>

        {/* Description content */}
        <Box
          sx={{ fontSize: 14, lineHeight: 1.6, py: 0.5 }}
          dangerouslySetInnerHTML={{ __html: product.description }}
        />

        {/* Error notification */}
        <Snackbar
          open={!!state.errorMessage}
          autoHideDuration={5000}
          anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
          onClose={this.emitDismissError}
        >
          <MuiAlert severity="error" elevation={6} variant="filled" onClose={this.emitDismissError}>
            {state.errorMessage}
          </MuiAlert>
        </Snackbar>

        {/* Back link */}
        <Link
          component="button"
          underline="always"
          onClick={this.emitGoHome}
          sx={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 0.5,
            color: '#1976d2',
            mt: 2,
            fontSize: '0.875rem',
            cursor: 'pointer',
          }}
        >
          <ArrowBackIcon fontSize="small" />
          Voltar aos produtos
        </Link>
      </Box>
    )
  }

  // :: Emissors

  readonly emitDismissError = () => {
    this.state.errorMessage = undefined
    this.forceUpdate()
  }

  readonly emitAddToCart = () => {
    const { vsid, state } = this
    app.setFormField(vsid, 'p.quantity', state.quantity)
    app.submit(vsid, ON_ADD_TO_CART)
  }

  readonly emitGoHome = () => {
    const { vsid } = this
    app.submit(vsid, ON_OPEN_PRODUCTS)
  }

  readonly emitIncrement = () => {
    const { vsid, state } = this
    state.quantity = (state.quantity ?? 1) + 1
    app.setFormField(vsid, 'quantity', state.quantity)
    this.forceUpdate()
  }

  readonly emitDecrement = () => {
    const { vsid, state } = this
    const cur = state.quantity ?? 1
    if (cur > 1) {
      state.quantity = cur - 1
      app.setFormField(vsid, 'quantity', state.quantity)
      this.forceUpdate()
    }
  }
}

export default BaseViewClass.FC(ProductViewClass, '48b693f67410')
