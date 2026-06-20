package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do recibo — espelha {@code views/receipt/_receipt.scss}. Herda os globais. */
public final class ReceiptSel extends Sel {

    public static final ReceiptSel INSTANCE = new ReceiptSel();

    private ReceiptSel() {
        // singleton
    }

    public final String RECEIPT_PAGE = "ReceiptPageRcp";
    public final String ALERT_SUCCESS = "AlertSuccessRcp";
    public final String ALERT_SUCCESS_ICON = "AlertSuccessIconRcp";
    public final String ALERT_SUCCESS_TEXT = "AlertSuccessTextRcp";
    public final String RECEIPT_CARD = "ReceiptCardRcp";
    public final String RECEIPT_BODY = "ReceiptBodyRcp";
    public final String RECEIPT_DATE_ROW = "ReceiptDateRowRcp";
    public final String RECEIPT_DATE_LABEL = "ReceiptDateLabelRcp";
    public final String RECEIPT_DATE_VALUE = "ReceiptDateValueRcp";
    public final String RECEIPT_TABLE_HEADER = "ReceiptTableHeaderRcp";
    public final String RECEIPT_COL_HEAD = "ReceiptColHeadRcp";
    public final String RECEIPT_COL_HEAD_QTY = "ReceiptColHeadQtyRcp";
    public final String RECEIPT_COL_HEAD_VALUE = "ReceiptColHeadValueRcp";
    public final String RECEIPT_ITEM_ROW = "ReceiptItemRowRcp";
    public final String RECEIPT_ITEM_DESC = "ReceiptItemDescRcp";
    public final String RECEIPT_ITEM_QTY = "ReceiptItemQtyRcp";
    public final String RECEIPT_ITEM_VALUE = "ReceiptItemValueRcp";
    public final String RECEIPT_TOTAL_ROW = "ReceiptTotalRowRcp";
    public final String RECEIPT_TOTAL_LABEL = "ReceiptTotalLabelRcp";
    public final String RECEIPT_TOTAL_VALUE = "ReceiptTotalValueRcp";
}
