package br.com.wdc.store.presentation.presenter.open.login;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.open.OpenKeys;

/**
 * The keys of the login place. It has no names of its own yet, but every place has its keys: it is where they will
 * go, and where navigating to the place starts. See {@link RootKeys}.
 */
public final class LoginKeys extends OpenKeys {

    public static final String PREFIX = "lg";

    public static final CubePlace PLACE = OpenKeys.PLACE.child("login", LoginPresenter::new);

    public LoginKeys(StoreApplication app) {
        this(app, null);
    }

    public LoginKeys(StoreApplication app, CubeIntent intent) {
        super(app, intent, PLACE);
    }
}
