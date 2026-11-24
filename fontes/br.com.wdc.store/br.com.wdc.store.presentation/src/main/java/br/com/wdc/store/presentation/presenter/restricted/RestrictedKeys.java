package br.com.wdc.store.presentation.presenter.restricted;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootKeys;

/**
 * The keys of the restricted branch (the places that need a user). A guard place: alone, it goes on to the home. See
 * {@link RootKeys}.
 */
public class RestrictedKeys extends RootKeys {

    public static final String PREFIX = "rs";

    public static final CubePlace PLACE = RootKeys.PLACE.child("restricted", RestrictedPresenter::new);

    public RestrictedKeys(StoreApplication app) {
        this(app, null);
    }

    public RestrictedKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected RestrictedKeys(StoreApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
