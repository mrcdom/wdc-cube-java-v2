package br.com.wdc.store.presentation.presenter.open;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootKeys;

/**
 * The keys of the open branch (the places with no user). A guard place: alone, it goes on to the login. See {@link RootKeys}.
 */
public class OpenKeys extends RootKeys {

    public static final String PREFIX = "op";

    public static final CubePlace PLACE = RootKeys.PLACE.child("open", OpenPresenter::new);

    public OpenKeys(StoreApplication app) {
        this(app, null);
    }

    public OpenKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected OpenKeys(StoreApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
