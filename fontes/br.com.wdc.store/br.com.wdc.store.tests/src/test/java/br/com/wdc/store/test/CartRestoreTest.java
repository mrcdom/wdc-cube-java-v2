package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import br.com.wdc.store.presentation.StorageKeys;
import br.com.wdc.store.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.mock.StoreApplicationMock;
import br.com.wdc.store.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.store.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.store.test.util.BasePresentationTest;

/** The cart kept in the session scope comes back when the application starts again (a page reload). */
public class CartRestoreTest extends BasePresentationTest {

    private static HomeViewMock signIn(StoreApplicationMock app, String user) {
        new LoginKeys(app).flip();
        var rootView = app.getRootView();
        var loginView = LoginViewMock.cast(app.viewOf(rootView.scope.content.get()));
        loginView.scope.userName.set(user);
        loginView.scope.onEnter.run(user, user);
        return HomeViewMock.cast(app.viewOf(rootView.scope.content.get()));
    }

    private static void addToCart(StoreApplicationMock app, HomeViewMock homeView, long productId, int quantity) {
        homeView.getProductsPanelScope().onOpenProduct.run(productId);
        var productView = ProductViewMock.cast(app.viewOf(homeView.scope.content.get()));
        productView.scope.onAddToCart.run(quantity);
    }

    /** A new application over the same session scope: what a reloaded page gets. */
    private StoreApplicationMock reload(String cart) {
        var reloaded = new StoreApplicationMock();
        if (cart != null) {
            reloaded.clientSessionStore().set(StorageKeys.CART, cart);
        }
        return reloaded;
    }

    @Test
    public void theCartIsKeptInTheSessionScope() {
        var homeView = signIn(this.app, "admin");
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 2);
        addToCart(this.app, homeView, DBReset.WILSON_BALL_ID, 1);

        assertEquals(DBReset.ADMIN_ID + "|" + DBReset.FLASH_DRIVE_2GB_ID + ":2," + DBReset.WILSON_BALL_ID + ":1",
                this.app.clientSessionStore().get(StorageKeys.CART));
    }

    @Test
    public void addingAProductAlreadyInTheCartUpdatesTheKeptCart() {
        var homeView = signIn(this.app, "admin");
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 1);
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 2);

        assertEquals(DBReset.ADMIN_ID + "|" + DBReset.FLASH_DRIVE_2GB_ID + ":3",
                this.app.clientSessionStore().get(StorageKeys.CART));
        assertEquals(Integer.valueOf(3), homeView.scope.cartItemCount.get());
    }

    @Test
    public void aReloadRestoresTheCartWithTheProductsReadAgain() {
        var homeView = signIn(this.app, "admin");
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 2);
        var kept = this.app.clientSessionStore().get(StorageKeys.CART);

        var reloaded = reload(kept);
        try {
            var reloadedHome = signIn(reloaded, "admin");

            assertEquals(Integer.valueOf(2), reloadedHome.scope.cartItemCount.get());
            var items = reloaded.getCart().getCartItems();
            assertEquals(1, items.size());
            assertEquals("2GB Flash Drive", items.get(0).name);
            assertEquals(16.0, items.get(0).price, 0.001);
            assertEquals(2, items.get(0).quantity);
        } finally {
            reloaded.release();
        }
    }

    @Test
    public void anotherUsersCartIsNotRestored() {
        var homeView = signIn(this.app, "admin");
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 2);
        var kept = this.app.clientSessionStore().get(StorageKeys.CART);

        var reloaded = reload(kept);
        try {
            var reloadedHome = signIn(reloaded, "jdoe");

            assertEquals(Integer.valueOf(0), reloadedHome.scope.cartItemCount.get());
            assertTrue(reloaded.getCart().getCartItems().isEmpty());
        } finally {
            reloaded.release();
        }
    }

    @Test
    public void signingOutForgetsTheKeptCart() {
        var homeView = signIn(this.app, "admin");
        addToCart(this.app, homeView, DBReset.FLASH_DRIVE_2GB_ID, 1);

        homeView.scope.onExit.run();

        assertNull(this.app.clientSessionStore().get(StorageKeys.CART));
    }

    @Test
    public void snapshotRoundTrip() {
        var snapshot = new CartManager.Snapshot(7L, Map.of(3L, 2));
        assertEquals("7|3:2", snapshot.format());
        assertEquals(snapshot, CartManager.Snapshot.parse("7|3:2"));
        assertEquals(new CartManager.Snapshot(7L, Map.of()), CartManager.Snapshot.parse("7|"));
    }

    @Test
    public void malformedSnapshotsAreIgnored() {
        assertNull(CartManager.Snapshot.parse(null));
        assertNull(CartManager.Snapshot.parse(""));
        assertNull(CartManager.Snapshot.parse("no bar"));
        assertNull(CartManager.Snapshot.parse("7|x:1"));
    }
}
