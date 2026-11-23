package br.com.wdc.shopping.presentation.presenter;

import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePlace;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.OpenPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.RestrictedPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;

/**
 * The application's place tree and a way to go to each place. The path a navigation walks comes from the tree: going to
 * {@link #CART} applies the intent to the presenters of {@code ROOT → RESTRICTED → HOME → CART}.
 */
public final class Routes {

    // :: Constructor. No one must create instance of this class.
    private Routes() {
        super();
    }

    // :: Place tree

    public static final CubePlace ROOT = CubePlace.root("public", RootPresenter::new);

    public static final CubePlace OPEN = ROOT.child("open", OpenPresenter::new);
    public static final CubePlace LOGIN = OPEN.child("login", LoginPresenter::new);

    public static final CubePlace RESTRICTED = ROOT.child("restricted", RestrictedPresenter::new);
    public static final CubePlace HOME = RESTRICTED.child("home", HomePresenter::new);
    public static final CubePlace CART = HOME.child("cart", CartPresenter::new);
    public static final CubePlace PRODUCT = HOME.child("product", ProductPresenter::new);
    public static final CubePlace RECEIPT = HOME.child("receipt", ReceiptPresenter::new);

    // :: Root: the login or the home, depending on whether there is a user

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

    // :: Open

    public static boolean open(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(OPEN, intent);
    }

    public static boolean login(ShoppingApplication app) {
        return login(app, app.newIntent());
    }

    public static boolean login(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(LOGIN, intent);
    }

    // :: Restricted

    public static boolean restricted(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(RESTRICTED, intent);
    }

    public static boolean home(ShoppingApplication app) {
        return home(app, app.newIntent());
    }

    public static boolean home(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(HOME, intent);
    }

    public static boolean cart(ShoppingApplication app) {
        return cart(app, app.newIntent());
    }

    public static boolean cart(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(CART, intent);
    }

    public static boolean product(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(PRODUCT, intent);
    }

    public static boolean receipt(ShoppingApplication app, CubeIntent intent) {
        return app.navigate(RECEIPT, intent);
    }
}
