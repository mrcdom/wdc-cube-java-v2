package br.com.wdc.shopping.presentation.presenter;

import java.util.function.Function;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.function.GoAction;
import br.com.wdc.shopping.presentation.presenter.open.OpenPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.RestrictedPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;

/**
 * This class is a singleton path factory for the application. Every time the application needs to go to a specific
 * place, this factory will provide a way to get a path that can walk through there.
 */
public final class Routes {

    // :: Constructor. No one must create instance of this class.
    private Routes() {
        super();
    }

    // :: Navigation

    public enum Place implements CubePlace {
        // Level 0
        ROOT("public", Routes::root, RootPresenter::new),

        // Level 1
        OPEN("open", Routes::open, OpenPresenter::new),
        RESTRICTED("restricted", Routes::restricted, RestrictedPresenter::new),

        // Level 2
        LOGIN("login", Routes::login, LoginPresenter::new),
        HOME("home", Routes::home, HomePresenter::new),

        // Level 3
        CART("cart", Routes::cart, CartPresenter::new),
        PRODUCT("product", Routes::product, ProductPresenter::new),
        RECEIPT("receipt", Routes::receipt, ReceiptPresenter::new);

        private final String name;
        private Function<ShoppingApplication, CubePresenter> factory;

        Place(String path, GoAction goAction, Function<ShoppingApplication, CubePresenter> factory) {
            this.name = path;
            this.factory = factory;
            ShoppingApplication.Internals.registerPlace(path, goAction);
        }

        @Override
        public Integer getId() {
            return this.ordinal();
        }

        @Override
        public String getName() {
            return name;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Function<ShoppingApplication, CubePresenter> presenterFactory() {
            return this.factory;
        }
    }

    // -- Level 0 --

    public static boolean root(ShoppingApplication app) {
        return root(app, app.newIntent());
    }

    public static boolean root(ShoppingApplication app, CubeIntent intent) {
        if (app.getSubject() == null) {
            return login(app, intent);
        } else {
            return home(app, intent);
        }
    }

    // -- Level 1 --

    public static boolean open(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.OPEN)
                .execute(intent);
    }

    public static boolean restricted(ShoppingApplication app, CubeIntent intent) {
        //@formatter:off
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.RESTRICTED)
                .execute(intent);
        //@formatter:on
    }

    // -- Level 2 --

    // :: Login

    public static boolean login(ShoppingApplication app) {
        return login(app, app.newIntent());
    }

    public static boolean login(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.OPEN)
                .step(Place.LOGIN)
                .execute(intent);
    }

    // :: Home

    public static boolean home(ShoppingApplication app) {
        return home(app, app.newIntent());
    }

    public static boolean home(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.RESTRICTED)
                .step(Place.HOME)
                .execute(intent);
    }

    // -- Level 3 --

    // :: Cart

    public static boolean cart(ShoppingApplication app) {
        return cart(app, app.newIntent());
    }

    public static boolean cart(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.RESTRICTED)
                .step(Place.HOME)
                .step(Place.CART)
                .execute(intent);
    }

    // :: Product

    public static boolean product(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.RESTRICTED)
                .step(Place.HOME)
                .step(Place.PRODUCT)
                .execute(intent);
    }

    // :: Receipt

    public static boolean receipt(ShoppingApplication app, CubeIntent intent) {
        return app.navigate()
                .step(Place.ROOT)
                .step(Place.RESTRICTED)
                .step(Place.HOME)
                .step(Place.RECEIPT)
                .execute(intent);
    }
}
