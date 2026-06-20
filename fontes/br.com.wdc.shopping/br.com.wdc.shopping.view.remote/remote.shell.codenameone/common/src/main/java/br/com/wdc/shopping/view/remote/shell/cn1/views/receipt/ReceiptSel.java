package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do recibo — espelha {@code views/receipt/_receipt.scss}. Herda os globais. */
public final class ReceiptSel extends Sel {

    public static final ReceiptSel INSTANCE = new ReceiptSel();

    private ReceiptSel() {
        // singleton
    }

    public final String RECEIPT_PAGE = "RcpReceiptPage";
    public final String ALERT_SUCCESS = "RcpAlertSuccess";
    public final String ALERT_SUCCESS_ICON = "RcpAlertSuccessIcon";
    public final String ALERT_SUCCESS_TEXT = "RcpAlertSuccessText";
    public final String RECEIPT_CARD = "RcpReceiptCard";
    public final String RECEIPT_BODY = "RcpReceiptBody";
    public final String RECEIPT_DATE_ROW = "RcpReceiptDateRow";
    public final String RECEIPT_DATE_LABEL = "RcpReceiptDateLabel";
    public final String RECEIPT_DATE_VALUE = "RcpReceiptDateValue";
    public final String RECEIPT_TABLE_HEADER = "RcpReceiptTableHeader";
    public final String RECEIPT_COL_HEAD = "RcpReceiptColHead";
    public final String RECEIPT_COL_HEAD_QTY = "RcpReceiptColHeadQty";
    public final String RECEIPT_COL_HEAD_VALUE = "RcpReceiptColHeadValue";
    public final String RECEIPT_ITEM_ROW = "RcpReceiptItemRow";
    public final String RECEIPT_ITEM_DESC = "RcpReceiptItemDesc";
    public final String RECEIPT_ITEM_QTY = "RcpReceiptItemQty";
    public final String RECEIPT_ITEM_VALUE = "RcpReceiptItemValue";
    public final String RECEIPT_TOTAL_ROW = "RcpReceiptTotalRow";
    public final String RECEIPT_TOTAL_LABEL = "RcpReceiptTotalLabel";
    public final String RECEIPT_TOTAL_VALUE = "RcpReceiptTotalValue";
}
