import React from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import Toolbar from '@mui/material/Toolbar'
import Typography from '@mui/material/Typography'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DeleteIcon from '@mui/icons-material/Delete'
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass, BasePanelClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as EndpointUtils from '@root/utils/EndpointUtils'

// :: Actions

const ON_BUY = 1
const ON_REMOVE_PRODUCT = 2
const ON_OPEN_PRODUCTS = 3

// :: Types

type ItemCarrinho = {
  id: number
  name: string
  price: number
  quantity: number
}

// :: View

export type CartViewState = {
  items: ItemCarrinho[]
  errorMessage?: string
}

class CartViewClass extends BaseViewClass<ViewProps, CartViewState> {
  override render({ className }: ViewProps) {
    const { vsid, state } = this

    let valorTotal = 0
    let carrinhoTotal = 0
    const items = state.items ?? []

    items.forEach((prod) => {
      carrinhoTotal += prod.quantity
      valorTotal += prod.price * prod.quantity
    })

    return (
      <Card elevation={3} sx={{ maxWidth: 900, mx: 'auto', my: 3 }}>
        <CardContent>
          {/* Header */}
          <Toolbar disableGutters sx={{ mb: 1 }}>
            <ShoppingCartIcon sx={{ mr: 1 }} />
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Carrinho{' '}
              <Typography component="span" variant="body2" color="text.secondary">
                ({NumberUtils.format(carrinhoTotal, 0)} {carrinhoTotal === 1 ? 'item' : 'itens'})
              </Typography>
            </Typography>
            <Typography variant="subtitle2" color="text.secondary">
              LISTA DE PRODUTOS
            </Typography>
          </Toolbar>

          <Divider sx={{ mb: 2 }} />

          {/* Table */}
          <Paper variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow sx={{ bgcolor: 'grey.100' }}>
                  <TableCell sx={{ fontWeight: 'bold' }}>Item</TableCell>
                  <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                    Valor unitário
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                    Quantidade
                  </TableCell>
                  <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                    Remover
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={4} align="center">
                      <Typography color="text.secondary" variant="body2" sx={{ py: 2 }}>
                        Carrinho vazio
                      </Typography>
                    </TableCell>
                  </TableRow>
                ) : (
                  items.map((prod) => (
                    <TableRow key={prod.id} hover>
                      <TableCell>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Box
                            component="img"
                            src={EndpointUtils.productImagePath(prod.id)}
                            alt={prod.name}
                            sx={{ width: 42, height: 40, objectFit: 'contain' }}
                          />
                          {prod.name}
                        </Box>
                      </TableCell>
                      <TableCell align="right">R$ {NumberUtils.format(prod.price)}</TableCell>
                      <TableCell align="center">{NumberUtils.format(prod.quantity, 0)}</TableCell>
                      <TableCell align="center">
                        <RemoveProductButton vsid={vsid} product={prod} />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </Paper>

          {/* Total */}
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2, mr: 1 }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 'bold' }}>
              VALOR TOTAL: R$ {NumberUtils.format(valorTotal)}
            </Typography>
          </Box>

          {/* Error */}
          {state.errorMessage && (
            <Alert severity="error" sx={{ mt: 1 }}>
              {state.errorMessage}
            </Alert>
          )}

          {/* Actions */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 3 }}>
            <Button variant="outlined" startIcon={<ArrowBackIcon />} onClick={this.emitClickVoltar}>
              Voltar
            </Button>
            {items.length > 0 && (
              <Button variant="contained" color="warning" onClick={this.emitClickFinalizar}>
                Finalizar pedido &rarr;
              </Button>
            )}
          </Box>
        </CardContent>
      </Card>
    )
  }

  // :: Emissors

  readonly emitClickFinalizar = () => {
    const { vsid } = this
    app.submit(vsid, ON_BUY)
  }

  readonly emitClickVoltar = () => {
    const { vsid } = this
    app.submit(vsid, ON_OPEN_PRODUCTS)
  }
}

export default BaseViewClass.FC(CartViewClass, '7eb485e5f843')

// :: RemoveProductButton Component

type RemoveProductButtonProps = {
  vsid: string
  product: ItemCarrinho
}

class RemoveProductButtonClass extends BasePanelClass<RemoveProductButtonProps> {
  vsid!: string
  product!: ItemCarrinho

  override render({ vsid, product }: RemoveProductButtonProps) {
    this.vsid = vsid
    this.product = product

    return (
      <IconButton size="small" color="error" onClick={this.emitRemoveProduct} aria-label={`Remover ${product.name}`}>
        <DeleteIcon fontSize="small" />
      </IconButton>
    )
  }

  readonly emitRemoveProduct = () => {
    const { vsid, product } = this
    app.setFormField(vsid, 'p.productId', product.id)
    app.submit(vsid, ON_REMOVE_PRODUCT)
  }
}

const RemoveProductButton = BasePanelClass.FC(RemoveProductButtonClass)
