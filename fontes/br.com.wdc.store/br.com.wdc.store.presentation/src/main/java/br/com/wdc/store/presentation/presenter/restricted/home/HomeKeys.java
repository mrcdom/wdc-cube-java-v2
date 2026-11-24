package br.com.wdc.store.presentation.presenter.restricted.home;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.RestrictedKeys;

/**
 * The keys of the home and of the places inside it. See {@link RootKeys}.
 */
public class HomeKeys extends RestrictedKeys {

    public static final String PREFIX = "hm";

    public static final CubePlace PLACE = RestrictedKeys.PLACE.child("home", HomePresenter::new);

    public HomeKeys(StoreApplication app) {
        this(app, null);
    }

    public HomeKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }

    protected HomeKeys(StoreApplication app, CubeIntent intent, CubePlace place) {
        super(app, intent, place);
    }
}
