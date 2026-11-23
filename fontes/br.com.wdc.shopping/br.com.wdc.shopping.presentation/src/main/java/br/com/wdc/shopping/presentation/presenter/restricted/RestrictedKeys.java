package br.com.wdc.shopping.presentation.presenter.restricted;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootKeys;

/**
 * The keys of the restricted branch (the places that need a user). A guard place: alone, it goes on to the home. See
 * {@link RootKeys}.
 */
public class RestrictedKeys extends RootKeys {

    public static final String PREFIX = "rs";

    public static final CubePlace PLACE = RootKeys.PLACE.child("restricted", RestrictedPresenter::new);

    public RestrictedKeys(ShoppingApplication app) {
        this(app, null);
    }

    public RestrictedKeys(ShoppingApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected RestrictedKeys(ShoppingApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
