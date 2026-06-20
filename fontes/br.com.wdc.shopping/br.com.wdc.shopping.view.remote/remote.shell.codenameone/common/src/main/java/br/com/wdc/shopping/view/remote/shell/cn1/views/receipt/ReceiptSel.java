package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do recibo — espelha {@code views/receipt/_receipt.scss}. Herda os globais. */
public final class ReceiptSel extends Sel {

    public static final ReceiptSel INSTANCE = new ReceiptSel();

    private ReceiptSel() {
        // singleton
    }

    public final String RECEIPT_PAGE = "RcpPage";
    public final String ALERT_SUCCESS = "RcpAlertSuccess";
    public final String ALERT_SUCCESS_ICON = "RcpAlertSuccessIcon";
    public final String ALERT_SUCCESS_TEXT = "RcpAlertSuccessText";
    public final String RECEIPT_CARD = "RcpCard";
    public final String RECEIPT_BODY = "RcpBody";
    public final String RECEIPT_DATE_ROW = "RcpDateRow";
    public final String RECEIPT_DATE_LABEL = "RcpDateLabel";
    public final String RECEIPT_DATE_VALUE = "RcpDateValue";
    public final String RECEIPT_TABLE_HEADER = "RcpTableHeader";
    public final String RECEIPT_COL_HEAD = "RcpColHead";
    public final String RECEIPT_COL_HEAD_QTY = "RcpColHeadQty";
    public final String RECEIPT_COL_HEAD_VALUE = "RcpColHeadValue";
    public final String RECEIPT_ITEM_ROW = "RcpItemRow";
    public final String RECEIPT_ITEM_DESC = "RcpItemDesc";
    public final String RECEIPT_ITEM_QTY = "RcpItemQty";
    public final String RECEIPT_ITEM_VALUE = "RcpItemValue";
    public final String RECEIPT_TOTAL_ROW = "RcpTotalRow";
    public final String RECEIPT_TOTAL_LABEL = "RcpTotalLabel";
    public final String RECEIPT_TOTAL_VALUE = "RcpTotalValue";
}
