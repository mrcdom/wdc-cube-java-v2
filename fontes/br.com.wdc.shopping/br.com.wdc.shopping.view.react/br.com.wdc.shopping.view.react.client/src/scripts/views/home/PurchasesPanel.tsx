import React, { ReactNode } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Divider from '@mui/material/Divider'
import FormControl from '@mui/material/FormControl'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import Typography from '@mui/material/Typography'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass, BasePanelClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as DateUtils from '@root/utils/DateUtils'

const PAGE_SIZE_OPTIONS = [2, 5, 10]

// :: Actions

const ON_OPEN_RECEIPT = 1
const ON_PAGE_CHANGE = 2
const ON_PAGE_SIZE_CHANGE = 3

// :: Types

export type Purchase = {
  id: number
  date: number
  total: number
  items: string[]
}

type PurchasesPanelState = {
  purchases?: Purchase[]
  page: number
  pageSize: number
  totalCount: number
}

// :: View

class PurchasesPanelClass extends BaseViewClass<ViewProps, PurchasesPanelState> {
  totalPages!: number

  // :: Renders

  override render({ className }: ViewProps): React.ReactNode {
    const { state } = this
    this.totalPages = Math.max(1, Math.ceil(state.totalCount / state.pageSize))

    return (
      <Paper
        className={className}
        elevation={2}
        sx={{
          width: 220,
          flexShrink: 0,
          bgcolor: '#676767',
          p: 1.5,
        }}
      >
        <Typography variant="subtitle2" sx={{ color: '#fff', mb: 1, fontWeight: 'bold' }}>
          Seu histórico de compras
        </Typography>
        {this.#renderCompras()}
        {state.totalCount > 0 && this.#renderPageNavigation()}
        {state.totalCount > state.pageSize && this.#renderRodape()}
      </Paper>
    )
  }

  #renderCompras(): ReactNode {
    const { vsid, state } = this

    return (state.purchases ?? []).map((compra) => (
      <CardHistoricoCompra key={compra.id} vsid={vsid} purchase={compra}>
        <List dense disablePadding>
          {compra.items.map((item) => (
            <ListItem key={item} disableGutters sx={{ py: 0 }}>
              <ListItemText primary={item} slotProps={{ primary: { variant: 'caption' } }} />
            </ListItem>
          ))}
        </List>
      </CardHistoricoCompra>
    ))
  }

  #renderPageNavigation(): ReactNode {
    const { state } = this
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mt: 1 }}>
        <IconButton
          size="small"
          disabled={state.page === 0}
          onClick={this.emitPreviousPage}
          sx={{ color: '#fff', '&.Mui-disabled': { color: '#999' } }}
        >
          <ChevronLeftIcon fontSize="small" />
        </IconButton>
        <Typography variant="caption" sx={{ color: '#fff' }}>
          {state.page + 1}/{this.totalPages}
        </Typography>
        <IconButton
          size="small"
          disabled={state.page >= this.totalPages - 1}
          onClick={this.emitNextPage}
          sx={{ color: '#fff', '&.Mui-disabled': { color: '#999' } }}
        >
          <ChevronRightIcon fontSize="small" />
        </IconButton>
      </Box>
    )
  }

  #renderRodape(): ReactNode {
    const { state } = this
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', mt: 0.5, gap: 0.5 }}>
        <Typography variant="caption" sx={{ color: '#fff' }}>
          Por página:
        </Typography>
        <FormControl size="small" variant="standard">
          <Select
            value={state.pageSize}
            onChange={this.emitPageSizeChange}
            sx={{ color: '#fff', fontSize: 12, '& .MuiSelect-icon': { color: '#fff' } }}
            disableUnderline
          >
            {PAGE_SIZE_OPTIONS.map((opt) => (
              <MenuItem key={opt} value={opt}>
                {opt}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Box>
    )
  }

  // :: Emissors

  readonly emitPageChange = (page: number) => {
    const { vsid } = this
    app.setFormField(vsid, 'p.page', page)
    app.submit(vsid, ON_PAGE_CHANGE)
  }

  readonly emitNextPage = () => {
    const { state } = this
    this.emitPageChange(state.page + 1)
  }

  readonly emitPreviousPage = () => {
    const { state } = this
    this.emitPageChange(state.page - 1)
  }

  readonly emitPageSizeChange = (e: { target: { value: unknown } }) => {
    const { vsid } = this
    app.setFormField(vsid, 'p.pageSize', Number(e.target.value))
    app.submit(vsid, ON_PAGE_SIZE_CHANGE)
  }
}

export default BaseViewClass.FC(PurchasesPanelClass, 'b3c4d5e6f7a8')

// :: Internal - CardHistoricoCompra

type CardHistoricoCompraProps = {
  vsid: string
  purchase: Purchase
  children?: ReactNode
}

class CardHistoricoCompraClass extends BasePanelClass<CardHistoricoCompraProps> {
  vsid!: string
  purchase!: Purchase

  override render({ vsid, purchase, children }: CardHistoricoCompraProps): React.ReactNode {
    this.vsid = vsid
    this.purchase = purchase

    return (
      <Card elevation={1} sx={{ mb: 1.5, bgcolor: '#fff' }}>
        <CardContent sx={{ p: 1, '&:last-child': { pb: 1 } }}>
          <Typography
            variant="caption"
            sx={{ bgcolor: '#e5e5e5', display: 'block', p: 0.5, mb: 0.5, fontWeight: 'bold' }}
          >
            Compra #{purchase.id}
          </Typography>
          <Typography variant="caption" sx={{ display: 'block', fontWeight: 'bold' }}>
            Data da compra:
          </Typography>
          <Typography variant="caption" sx={{ display: 'block', mb: 0.5 }}>
            {DateUtils.formatDate(purchase.date)}
          </Typography>
          <Typography variant="caption" sx={{ display: 'block', fontWeight: 'bold' }}>
            Itens adquiridos:
          </Typography>
          {children}
          <Divider sx={{ my: 0.5 }} />
          <Typography variant="caption" sx={{ display: 'block' }}>
            <strong>Valor Total: </strong>R$ {NumberUtils.format(purchase.total)}
          </Typography>
          <Box sx={{ textAlign: 'right', mt: 0.5 }}>
            <Button
              size="small"
              variant="contained"
              color="warning"
              onClick={this.emitOpenReceipt}
              sx={{ fontSize: 10 }}
            >
              Ver mais detalhes
            </Button>
          </Box>
        </CardContent>
      </Card>
    )
  }

  // :: Emissors

  readonly emitOpenReceipt = () => {
    const { vsid, purchase } = this
    app.setFormField(vsid, 'p.purchaseId', purchase.id)
    app.submit(vsid, ON_OPEN_RECEIPT)
  }
}

const CardHistoricoCompra = BasePanelClass.FC(CardHistoricoCompraClass)
