package br.com.wdc.store.presentation.presenter.restricted.cart;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

/**
 * The keys of the cart place. It has no names of its own yet, but every place has its keys: it is where they will
 * go, and where navigating to the place starts. See {@link RootKeys}.
 */
public final class CartKeys extends HomeKeys {

    public static final String PREFIX = "ct";

    public static final CubePlace PLACE = HomeKeys.PLACE.child("cart", CartPresenter::new);

    public CartKeys(StoreApplication app) {
        this(app, null);
    }

    public CartKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }
}
