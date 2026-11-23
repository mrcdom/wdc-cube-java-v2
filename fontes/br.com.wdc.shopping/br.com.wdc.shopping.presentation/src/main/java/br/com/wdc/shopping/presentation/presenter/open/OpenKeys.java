package br.com.wdc.shopping.presentation.presenter.open;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootKeys;

/**
 * The keys of the open branch (the places with no user). A guard place: alone, it goes on to the login. See {@link RootKeys}.
 */
public class OpenKeys extends RootKeys {

    public static final String PREFIX = "op";

    public static final CubePlace PLACE = RootKeys.PLACE.child("open", OpenPresenter::new);

    public OpenKeys(ShoppingApplication app) {
        this(app, null);
    }

    public OpenKeys(ShoppingApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected OpenKeys(ShoppingApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
