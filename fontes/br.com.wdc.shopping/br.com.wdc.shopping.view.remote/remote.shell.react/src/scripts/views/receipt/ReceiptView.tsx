import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { ActionButton } from "@root/swc"
import * as NumberUtils from "@root/utils/NumberUtils"
import Sel from "./receipt-sel"

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
      <div className={clsx(Sel.pageScrollRoot, className)}>
        <div className={Sel.pageWrapper}>
          {/* Success alert */}
          {state.notifySuccess && (
            <div className={Sel.alertSuccess}>
              <span className={Sel.alertSuccessIcon}>
                <i className="bi bi-check-circle-fill"></i>
              </span>
              <span className={Sel.alertSuccessText}>Compra realizada com sucesso!</span>
            </div>
          )}

          {/* Receipt card */}
          <div className={Sel.cardPanelLg}>
            {/* Header */}
            <div className={Sel.cardHeaderRow}>
              <div className={Sel.cardHeaderIconBox}>
                <i className={clsx("bi bi-receipt", Sel.cardHeaderIcon)}></i>
              </div>
              <div>
                <h5 className={Sel.cardHeaderTitle}>Recibo de Compra</h5>
                <span className={Sel.cardHeaderSubtitle}>WDC Shopping</span>
              </div>
            </div>

            {/* Receipt body */}
            <div className={Sel.body}>
              {/* Date */}
              <div className={Sel.dateRow}>
                <span className={Sel.dateLabel}>Data:</span>
                <span className={Sel.dateValue}>
                  {state.receipt?.date
                    ? new Date(state.receipt.date).toLocaleDateString("pt-BR") +
                      " " +
                      new Date(state.receipt.date).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })
                    : ""}
                </span>
              </div>

              {/* Items table */}
              <div className={Sel.tableHeader}>
                <span className={Sel.colItem}>ITEM</span>
                <span className={Sel.colQty}>QTD</span>
                <span className={Sel.colValue}>VALOR</span>
              </div>
              <div>
                {reciboItems.map((item, idx) => (
                  <div key={idx} className={Sel.itemRow}>
                    <span className={Sel.itemDesc}>{item.description}</span>
                    <span className={Sel.itemQty}>{NumberUtils.format(item.quantity, 0)}</span>
                    <span className={Sel.itemValue}>R$ {NumberUtils.format(item.value)}</span>
                  </div>
                ))}
              </div>

              {/* Total */}
              <div className={Sel.totalRow}>
                <span className={Sel.totalLabel}>TOTAL:</span>
                <span className={Sel.totalValue}>R$ {NumberUtils.format(state.receipt?.total ?? 0)}</span>
              </div>
            </div>

            {/* Back button */}
            <ActionButton quiet className={Sel.backBtn} onClick={this.emitOpenProducts}>
              <i className={clsx("bi bi-arrow-left", Sel.mr4)}></i>
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

export default BaseViewClass.FC(ReceiptViewClass, "e8d0bd8ae3bc")
