package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do recibo — espelha {@code views/receipt/_receipt.scss}. Herda os globais. */
public final class ReceiptSel extends Sel {

    public static final ReceiptSel INSTANCE = new ReceiptSel();

    private ReceiptSel() {
        // singleton
    }

    public final String RECEIPT_PAGE = "ReceiptPage";
    public final String ALERT_SUCCESS = "AlertSuccess";
    public final String ALERT_SUCCESS_ICON = "AlertSuccessIcon";
    public final String ALERT_SUCCESS_TEXT = "AlertSuccessText";
    public final String RECEIPT_CARD = "ReceiptCard";
    public final String RECEIPT_BODY = "ReceiptBody";
    public final String RECEIPT_DATE_ROW = "ReceiptDateRow";
    public final String RECEIPT_DATE_LABEL = "ReceiptDateLabel";
    public final String RECEIPT_DATE_VALUE = "ReceiptDateValue";
    public final String RECEIPT_TABLE_HEADER = "ReceiptTableHeader";
    public final String RECEIPT_COL_HEAD = "ReceiptColHead";
    public final String RECEIPT_COL_HEAD_QTY = "ReceiptColHeadQty";
    public final String RECEIPT_COL_HEAD_VALUE = "ReceiptColHeadValue";
    public final String RECEIPT_ITEM_ROW = "ReceiptItemRow";
    public final String RECEIPT_ITEM_DESC = "ReceiptItemDesc";
    public final String RECEIPT_ITEM_QTY = "ReceiptItemQty";
    public final String RECEIPT_ITEM_VALUE = "ReceiptItemValue";
    public final String RECEIPT_TOTAL_ROW = "ReceiptTotalRow";
    public final String RECEIPT_TOTAL_LABEL = "ReceiptTotalLabel";
    public final String RECEIPT_TOTAL_VALUE = "ReceiptTotalValue";
}
