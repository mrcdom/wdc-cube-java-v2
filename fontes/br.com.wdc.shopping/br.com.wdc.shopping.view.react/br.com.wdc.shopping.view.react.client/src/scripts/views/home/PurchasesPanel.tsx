import React from 'react'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft'
import ChevronRightIcon from '@mui/icons-material/ChevronRight'
import app, { type ViewProps } from '@root/App'
import { BaseViewClass, BasePanelClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as DateUtils from '@root/utils/DateUtils'

const ITEM_HEIGHT_PX = 50

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
  private resizeHandler: (() => void) | null = null
  private resizeTimer: number | null = null
  private listRef = React.createRef<HTMLDivElement>()

  private computePageSize = () => {
    const el = this.listRef.current
    if (!el) return
    const containerHeight = el.clientHeight
    if (containerHeight <= 0) return
    let itemHeight = ITEM_HEIGHT_PX
    const firstItem = el.querySelector('[data-purchase-item]') as HTMLElement | null
    if (firstItem && firstItem.offsetHeight > 0) {
      const style = getComputedStyle(firstItem)
      itemHeight = firstItem.offsetHeight + parseFloat(style.marginTop) + parseFloat(style.marginBottom)
    }
    const capacity = Math.max(1, Math.floor(containerHeight / itemHeight))
    const { vsid } = this
    app.setFormField(vsid, 'p.capacity', capacity)
    app.submitSilent(vsid, ON_PAGE_SIZE_CHANGE)
  }

  private onResize = () => {
    if (this.resizeTimer != null) clearTimeout(this.resizeTimer)
    this.resizeTimer = window.setTimeout(this.computePageSize, 150)
  }

  // :: Renders

  override render({ className }: ViewProps, initial?: boolean): React.ReactNode {
    const { state } = this
    const pageSize = Math.max(1, state.pageSize)
    this.totalPages = Math.max(1, Math.ceil(state.totalCount / pageSize))

    if (initial && !this.resizeHandler) {
      this.resizeHandler = this.onResize
      requestAnimationFrame(this.computePageSize)
      window.addEventListener('resize', this.onResize)
    }

    return (
      <Paper
        className={className}
        elevation={0}
        sx={{
          width: 300,
          flexShrink: 0,
          bgcolor: '#fff',
          border: '1px solid',
          borderColor: 'grey.300',
          borderRadius: 2,
          m: 2,
          ml: 0,
          p: 2,
          display: 'flex',
          flexDirection: 'column',
          alignSelf: 'stretch',
        }}
      >
        <Typography variant="subtitle2" sx={{ color: 'primary.main', mb: 1.5, fontWeight: 'bold' }}>
          Seu histórico de compras
        </Typography>
        <Box ref={this.listRef} sx={{ flex: 1, overflow: 'hidden' }}>
          {this.#renderCompras()}
        </Box>
        {state.totalCount > 0 && this.#renderPageNavigation()}
      </Paper>
    )
  }

  #renderCompras(): React.ReactNode {
    const { vsid, state } = this

    return (state.purchases ?? []).map((compra) => <PurchaseItemRow key={compra.id} vsid={vsid} purchase={compra} />)
  }

  #renderPageNavigation(): React.ReactNode {
    const { state } = this
    return (
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mt: 1,
          pt: 1,
          borderTop: '1px solid',
          borderColor: 'grey.200',
        }}
      >
        <IconButton
          size="small"
          disabled={state.page === 0}
          onClick={this.emitPreviousPage}
          sx={{ color: 'primary.main', '&.Mui-disabled': { color: 'grey.400' } }}
        >
          <ChevronLeftIcon fontSize="small" />
        </IconButton>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {state.page + 1}/{this.totalPages}
        </Typography>
        <IconButton
          size="small"
          disabled={state.page >= this.totalPages - 1}
          onClick={this.emitNextPage}
          sx={{ color: 'primary.main', '&.Mui-disabled': { color: 'grey.400' } }}
        >
          <ChevronRightIcon fontSize="small" />
        </IconButton>
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
}

export default BaseViewClass.FC(PurchasesPanelClass, 'b3c4d5e6f7a8')

// :: Internal - PurchaseItemRow (compact)

type PurchaseItemRowProps = {
  vsid: string
  purchase: Purchase
}

function formatItems(items: string[]): string {
  if (!items || items.length === 0) return ''
  return items.join(', ')
}

class PurchaseItemRowClass extends BasePanelClass<PurchaseItemRowProps> {
  vsid!: string
  purchase!: Purchase

  override render({ vsid, purchase }: PurchaseItemRowProps): React.ReactNode {
    this.vsid = vsid
    this.purchase = purchase

    return (
      <Box
        data-purchase-item
        onClick={this.emitOpenReceipt}
        sx={{
          mb: 0.75,
          borderRadius: 1,
          overflow: 'hidden',
          cursor: 'pointer',
          bgcolor: 'grey.50',
          borderLeft: '3px solid',
          borderColor: 'primary.main',
          transition: 'all 0.15s',
          '&:hover': {
            bgcolor: 'primary.50',
            borderColor: 'primary.dark',
            transform: 'translateX(2px)',
          },
        }}
      >
        {/* Line 1: #id + date */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 1.5, pt: 0.75 }}>
          <Typography variant="caption" sx={{ color: 'primary.main', fontWeight: 'bold', fontSize: '0.75rem' }}>
            #{purchase.id}
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.disabled', fontSize: '0.7rem' }}>
            {DateUtils.formatDate(purchase.date)}
          </Typography>
        </Box>
        {/* Line 2: products + total */}
        <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 0.5, px: 1.5, pb: 0.75 }}>
          <Typography
            variant="caption"
            sx={{
              color: 'text.secondary',
              flex: 1,
              minWidth: 0,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              fontSize: '0.75rem',
            }}
          >
            {formatItems(purchase.items)}
          </Typography>
          <Typography
            variant="caption"
            sx={{ color: 'text.primary', fontWeight: 'bold', whiteSpace: 'nowrap', fontSize: '0.75rem' }}
          >
            R$ {NumberUtils.format(purchase.total)}
          </Typography>
        </Box>
      </Box>
    )
  }

  readonly emitOpenReceipt = () => {
    const { vsid, purchase } = this
    app.setFormField(vsid, 'p.purchaseId', purchase.id)
    app.submit(vsid, ON_OPEN_RECEIPT)
  }
}

const PurchaseItemRow = BasePanelClass.FC(PurchaseItemRowClass)
