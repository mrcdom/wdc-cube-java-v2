package br.com.wdc.store.presentation.presenter.restricted.products;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

/**
 * The keys of the product place. See {@link RootKeys}.
 */
public final class ProductKeys extends HomeKeys {

    public static final String PREFIX = "pr";

    public static final CubePlace PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new);

    private static final class P {
        /** The product shown. */
        static final String PRODUCT_ID = PREFIX + "-id";
    }

    public ProductKeys(StoreApplication app) {
        this(app, null);
    }

    public ProductKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    /** An id (0 or more), or {@code null} when the address carries none it can use. */
    public Long productId() {
        var id = intent().getParameterAsLong(P.PRODUCT_ID, null);
        return id != null && id >= 0 ? id : null;
    }

    public ProductKeys withProductId(Long id) {
        intent().setParameter(P.PRODUCT_ID, id != null && id >= 0 ? id : null);
        return this;
    }
}
