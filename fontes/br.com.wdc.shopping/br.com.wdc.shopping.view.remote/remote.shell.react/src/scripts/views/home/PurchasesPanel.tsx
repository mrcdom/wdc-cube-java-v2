import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass, BasePanelClass } from '@root/utils/ViewUtils'
import * as NumberUtils from '@root/utils/NumberUtils'
import * as DateUtils from '@root/utils/DateUtils'

const ITEM_HEIGHT_PX = 56

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
    if (containerHeight <= 0) {
      window.setTimeout(() => requestAnimationFrame(this.computePageSize), 200)
      return
    }
    let itemHeight = ITEM_HEIGHT_PX
    const firstItem = el.querySelector('[data-purchase-item]') as HTMLElement | null
    if (firstItem && firstItem.offsetHeight > 0) {
      const style = getComputedStyle(firstItem)
      itemHeight = firstItem.offsetHeight + parseFloat(style.marginTop) + parseFloat(style.marginBottom)
    }
    const capacity = Math.max(1, Math.floor(containerHeight / itemHeight))
    if (capacity === this.state.pageSize) return
    const { vsid } = this
    bridge.setFormField(vsid, 'p.capacity', capacity)
    bridge.submit(vsid, ON_PAGE_SIZE_CHANGE)
  }

  private onResize = () => {
    if (this.resizeTimer != null) clearTimeout(this.resizeTimer)
    this.resizeTimer = window.setTimeout(this.computePageSize, 150)
  }

  // :: Renders

  override render({ className }: ViewProps, initial?: boolean): React.ReactNode {
    const { state } = this
    const pageSize = Math.max(1, state.pageSize || 1)
    const totalCount = state.totalCount || 0
    const page = state.page || 0
    this.totalPages = Math.max(1, Math.ceil(totalCount / pageSize))

    if (initial && !this.resizeHandler) {
      this.resizeHandler = this.onResize
      requestAnimationFrame(this.computePageSize)
      window.addEventListener('resize', this.onResize)
    } else if (!state.pageSize) {
      requestAnimationFrame(this.computePageSize)
    }

    return (
      <div className={`purchases-panel slot-purchases ${className || ''}`}>
        <div className="purchases-header-row">
          <i className="bi bi-clock-history purchases-header-icon"></i>
          <span className="purchases-header-title">Histórico</span>
        </div>
        <span className="purchases-hint">Toque para ver detalhes</span>
        <div ref={this.listRef} className="purchases-list-container">
          {this.#renderCompras()}
        </div>
        {totalCount > 0 && this.#renderPageNavigation()}
      </div>
    )
  }

  #renderCompras(): React.ReactNode {
    const { vsid, state } = this

    return (state.purchases ?? []).map((compra) => <PurchaseItemRow key={compra.id} vsid={vsid} purchase={compra} />)
  }

  #renderPageNavigation(): React.ReactNode {
    const page = this.state.page || 0
    return (
      <div className="purchases-pagination">
        <div className="purchases-page-pill">
          <div
            className={`purchases-page-btn ${page === 0 ? 'pointer-events-none' : ''}`}
            onClick={page > 0 ? this.emitPreviousPage : undefined}
          >
            <i className="bi bi-chevron-left purchases-page-btn-icon"></i>
          </div>
          <span className="purchases-page-info">
            {page + 1} / {this.totalPages}
          </span>
          <div
            className={`purchases-page-btn ${page >= this.totalPages - 1 ? 'pointer-events-none' : ''}`}
            onClick={page < this.totalPages - 1 ? this.emitNextPage : undefined}
          >
            <i className="bi bi-chevron-right purchases-page-btn-icon"></i>
          </div>
        </div>
      </div>
    )
  }

  // :: Emissors

  readonly emitPageChange = (page: number) => {
    const { vsid } = this
    bridge.setFormField(vsid, 'p.page', page)
    bridge.submit(vsid, ON_PAGE_CHANGE)
  }

  readonly emitNextPage = () => {
    const page = this.state.page || 0
    this.emitPageChange(page + 1)
  }

  readonly emitPreviousPage = () => {
    const page = this.state.page || 0
    this.emitPageChange(page - 1)
  }
}

export default BaseViewClass.FC(PurchasesPanelClass, 'b3c4d5e6f7a8')

// :: Internal - PurchaseItemRow

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
      <div data-purchase-item className="purchases-item-card purchase-item" onClick={this.emitOpenReceipt}>
        {/* Line 1: #id + date */}
        <div className="purchases-item-line1">
          <span className="purchases-item-id">#{purchase.id}</span>
          <span className="purchases-item-date">{DateUtils.formatDate(purchase.date)}</span>
        </div>
        {/* Line 2: products + total */}
        <div className="purchases-item-line2">
          <span className="purchases-item-items">{formatItems(purchase.items)}</span>
          <span className="purchases-item-total">R$ {NumberUtils.format(purchase.total)}</span>
        </div>
      </div>
    )
  }

  readonly emitOpenReceipt = () => {
    const { vsid, purchase } = this
    bridge.setFormField(vsid, 'p.purchaseId', purchase.id)
    bridge.submit(vsid, ON_OPEN_RECEIPT)
  }
}

const PurchaseItemRow = BasePanelClass.FC(PurchaseItemRowClass)
