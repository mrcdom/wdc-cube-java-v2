package br.com.wdc.shopping.presentation.presenter.restricted.home.structs;

import java.io.Serializable;
import java.util.List;

@SuppressWarnings("java:S1948")
public class PurchaseInfo implements Serializable {

    private static final long serialVersionUID = 5212741192677222435L;

    public long id;
    public long date;
    public double total;
    public List<String> items;

}
