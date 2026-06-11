/**
 * Seletores CSS do módulo de recibo.
 * Importado por ReceiptView.tsx — não referenciar receipt.scss diretamente nas views.
 */
import GlobalSel from "@root/global-sel"

import "./receipt.scss"

const ReceiptSel = {
  ...GlobalSel,

  // :: Receipt

  headerTitle: "wdc-receipt__header-title",
  body: "wdc-receipt__body",
  dateRow: "wdc-receipt__date-row",
  dateLabel: "wdc-receipt__date-label",
  dateValue: "wdc-receipt__date-value",
  tableHeader: "wdc-receipt__table-header",
  colItem: "wdc-receipt__col-item",
  colQty: "wdc-receipt__col-qty",
  colValue: "wdc-receipt__col-value",
  itemRow: "wdc-receipt__item-row",
  itemDesc: "wdc-receipt__item-desc",
  itemQty: "wdc-receipt__item-qty",
  itemValue: "wdc-receipt__item-value",
  totalRow: "wdc-receipt__total-row",
  totalLabel: "wdc-receipt__total-label",
  totalValue: "wdc-receipt__total-value",
  backBtn: "wdc-receipt__back-btn",
} as const

export default ReceiptSel
