package br.com.wdc.shopping.presentation.presenter.restricted.home;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.RestrictedKeys;

/**
 * The keys of the home and of the places inside it. See {@link RootKeys}.
 */
public class HomeKeys extends RestrictedKeys {

    public static final String PREFIX = "hm";

    public static final CubePlace PLACE = RestrictedKeys.PLACE.child("home", HomePresenter::new);

    public HomeKeys(ShoppingApplication app) {
        this(app, null);
    }

    public HomeKeys(ShoppingApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected HomeKeys(ShoppingApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
