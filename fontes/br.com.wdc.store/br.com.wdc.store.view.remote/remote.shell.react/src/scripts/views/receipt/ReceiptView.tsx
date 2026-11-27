import React from "react"
import clsx from "clsx"
import bridge, { type ViewProps } from "@root/bridge"
import { BaseViewClass } from "@root/utils/ViewUtils"
import { ActionButton } from "@root/swc"
import * as NumberUtils from "@root/utils/NumberUtils"
import * as DateUtils from "@root/utils/DateUtils"
import Sel from "./receipt-sel"
import { t } from "@root/utils/I18n"

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

export type ReceiptScope = {
  receipt: ReceiptForm
  notifySuccess?: boolean
}

class ReceiptViewClass extends BaseViewClass<ViewProps, ReceiptScope> {
  // :: Renderes

  override render({ className }: ViewProps) {
    const { scope } = this
    const receiptItems = scope.receipt?.items ?? []

    return (
      <div className={clsx(Sel.pageScrollRoot, className)}>
        <div className={Sel.pageWrapper}>
          {/* Success alert */}
          {scope.notifySuccess && (
            <div className={Sel.alertSuccess}>
              <span className={Sel.alertSuccessIcon}>
                <i className="bi bi-check-circle-fill"></i>
              </span>
              <span className={Sel.alertSuccessText}>{t("receipt.completed")}</span>
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
                <h5 className={Sel.cardHeaderTitle}>{t("receipt.title")}</h5>
                <span className={Sel.cardHeaderSubtitle}>{t("app.name")}</span>
              </div>
            </div>

            {/* Receipt body */}
            <div className={Sel.body}>
              {/* Date */}
              <div className={Sel.dateRow}>
                <span className={Sel.dateLabel}>{t("receipt.date")}</span>
                <span className={Sel.dateValue}>
                  {scope.receipt?.date ? DateUtils.formatDateTime(scope.receipt.date) : ""}
                </span>
              </div>

              {/* Items table */}
              <div className={Sel.tableHeader}>
                <span className={Sel.colItem}>{t("receipt.item")}</span>
                <span className={Sel.colQty}>{t("receipt.quantity")}</span>
                <span className={Sel.colValue}>{t("receipt.price")}</span>
              </div>
              <div>
                {receiptItems.map((item, idx) => (
                  <div key={idx} className={Sel.itemRow}>
                    <span className={Sel.itemDesc}>{item.description}</span>
                    <span className={Sel.itemQty}>{NumberUtils.format(item.quantity, 0)}</span>
                    <span className={Sel.itemValue}>{NumberUtils.formatCurrency(item.value)}</span>
                  </div>
                ))}
              </div>

              {/* Total */}
              <div className={Sel.totalRow}>
                <span className={Sel.totalLabel}>{t("receipt.total")}</span>
                <span className={Sel.totalValue}>{NumberUtils.formatCurrency(scope.receipt?.total ?? 0)}</span>
              </div>
            </div>

            {/* Back button */}
            <ActionButton quiet className={Sel.backBtn} onClick={this.emitOpenProducts}>
              <i className={clsx("bi bi-arrow-left", Sel.mr4)}></i>
              {t("receipt.backToProducts")}
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
