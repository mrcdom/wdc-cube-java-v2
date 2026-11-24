package br.com.wdc.store.presentation.presenter.restricted.receipt;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

/**
 * The keys of the receipt place. See {@link RootKeys}.
 */
public final class ReceiptKeys extends HomeKeys {

    public static final String PREFIX = "rc";

    public static final CubePlace PLACE = HomeKeys.PLACE.child("receipt", ReceiptPresenter::new);

    private static final class P {
        /** The purchase whose receipt is shown. */
        static final String PURCHASE_ID = PREFIX + "-id";
    }

    private static final class A {
        /** Set by the cart right after the purchase: the receipt congratulates the buyer. */
        static final String PURCHASE_MADE = PREFIX + "-purchase-made";
    }

    public ReceiptKeys(StoreApplication app) {
        this(app, null);
    }

    public ReceiptKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    /** An id (0 or more), or {@code null} when the address carries none it can use. */
    public Long purchaseId() {
        var id = intent().getParameterAsLong(P.PURCHASE_ID, null);
        return id != null && id >= 0 ? id : null;
    }

    public ReceiptKeys withPurchaseId(Long id) {
        intent().setParameter(P.PURCHASE_ID, id != null && id >= 0 ? id : null);
        return this;
    }

    /** Whether the receipt comes right after the purchase (not from the address). */
    public boolean purchaseMade() {
        return Boolean.TRUE.equals(intent().getAttribute(A.PURCHASE_MADE));
    }

    public ReceiptKeys withPurchaseMade(boolean made) {
        intent().setAttribute(A.PURCHASE_MADE, made ? Boolean.TRUE : null);
        return this;
    }
}
