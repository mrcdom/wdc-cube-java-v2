package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

/** What the receipt draws ({@link ReceiptPresenter}): the purchase and whether it was just made. */
public class ReceiptScope extends Scope {

    public static final String CLASS_ID = "e8d0bd8ae3bc";

    public final Prop<Boolean> notifySuccess = prop(false);
    public final Prop<ReceiptForm> receipt = prop();

    public Action onOpenProducts = Action.NOOP;

}
