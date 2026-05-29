package br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("java:S1948")
public class ReceiptForm implements Serializable {

    private static final long serialVersionUID = -3783933804174247490L;

    public Long date;
    public List<ReceiptItem> items;
    public Double total;

}
