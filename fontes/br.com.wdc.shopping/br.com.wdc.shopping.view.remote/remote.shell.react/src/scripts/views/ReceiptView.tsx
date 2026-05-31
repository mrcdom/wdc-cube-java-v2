import React from 'react'
import bridge, { type ViewProps } from '@root/bridge'
import { BaseViewClass } from '@root/utils/ViewUtils'
import { ActionButton } from '@root/swc'
import * as NumberUtils from '@root/utils/NumberUtils'

// :: Actions

const ON_OPEN_PRODUCTS = 1

// :: Types

type ReceiptItem = {
  description: string
  value: number
  quantity: number
}

type ReceiptForm = {
  date: number
  items: ReceiptItem[]
  total: number
}

// :: View

export type ReceiptViewState = {
  receipt: ReceiptForm
  notifySuccess?: boolean
}

class ReceiptViewClass extends BaseViewClass<ViewProps, ReceiptViewState> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { state } = this
    const reciboItems = state.receipt?.items ?? []

    return (
      <div className={`page-scroll-root ${className || ''}`}>
        <div className="page-wrapper">
          {/* Success alert */}
          {state.notifySuccess && (
            <div className="alert-success">
              <span className="alert-success-icon">
                <i className="bi bi-check-circle-fill"></i>
              </span>
              <span className="alert-success-text">Compra realizada com sucesso!</span>
            </div>
          )}

          {/* Receipt card */}
          <div className="card-panel-lg">
            {/* Header */}
            <div className="card-header-row">
              <div className="card-header-icon-box">
                <i className="bi bi-receipt card-header-icon"></i>
              </div>
              <div>
                <h5 className="card-header-title">Recibo de Compra</h5>
                <span className="card-header-subtitle">WDC Shopping</span>
              </div>
            </div>

            {/* Receipt body */}
            <div className="receipt-body">
              {/* Date */}
              <div className="receipt-date-row">
                <span className="receipt-date-label">Data:</span>
                <span className="receipt-date-value">
                  {state.receipt?.date
                    ? new Date(state.receipt.date).toLocaleDateString('pt-BR') +
                      ' ' +
                      new Date(state.receipt.date).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
                    : ''}
                </span>
              </div>

              {/* Items table */}
              <div className="receipt-table-header">
                <span className="receipt-col-item">ITEM</span>
                <span className="receipt-col-qty">QTD</span>
                <span className="receipt-col-value">VALOR</span>
              </div>
              <div>
                {reciboItems.map((item, idx) => (
                  <div key={idx} className="receipt-item-row">
                    <span className="receipt-item-desc">{item.description}</span>
                    <span className="receipt-item-qty">{NumberUtils.format(item.quantity, 0)}</span>
                    <span className="receipt-item-value">R$ {NumberUtils.format(item.value)}</span>
                  </div>
                ))}
              </div>

              {/* Total */}
              <div className="receipt-total-row">
                <span className="receipt-total-label">TOTAL:</span>
                <span className="receipt-total-value">R$ {NumberUtils.format(state.receipt?.total ?? 0)}</span>
              </div>
            </div>

            {/* Back button */}
            <ActionButton quiet className="receipt-back-btn" onClick={this.emitOpenProducts}>
              <i className="bi bi-arrow-left mr-4"></i>
              Voltar aos produtos
            </ActionButton>
          </div>
        </div>
      </div>
    )
  }

  // :: Emissors

  readonly emitOpenProducts = () => {
    const { vsid } = this
    bridge.submit(vsid, ON_OPEN_PRODUCTS)
  }
}

export default BaseViewClass.FC(ReceiptViewClass, 'e8d0bd8ae3bc')
