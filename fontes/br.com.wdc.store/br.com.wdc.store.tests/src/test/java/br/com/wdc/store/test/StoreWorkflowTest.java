package br.com.wdc.store.test;

import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.store.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.mock.viewimpl.CartViewMock;
import br.com.wdc.store.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.store.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.store.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.store.test.util.BasePresentationTest;

public class StoreWorkflowTest extends BasePresentationTest {

    private HomeViewMock gotoRestricted() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        loginView.state.userName.set("admin");
        loginView.state.onEnter.run("admin", "admin");

        return HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
    }

    @Test
    public void testViewNonexistentProduct() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        // A product that does not exist
        restrictedView.getProductsPanelState().onOpenProduct.run(Long.MIN_VALUE);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        Assert.assertEquals("A negative id is not a product: " + restrictedView.state.errorCode.get(),
                6, (int) restrictedView.state.errorCode.get());

        restrictedView.getProductsPanelState().onOpenProduct.run(999_999L);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        Assert.assertEquals("The error code must indicate that the product does not exist: " + restrictedView.state.errorCode.get(),
                3, (int) restrictedView.state.errorCode.get());
    }

    @Test
    public void testViewProduct() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        restrictedView.getProductsPanelState().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));

        var productView = ProductViewMock.cast(this.app.viewOf(restrictedView.state.content.get()));
        Assert.assertNotNull("A product should have been selected", productView.state.product.get());
    }

    @Test
    public void testBuyProduct() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        loginView.state.userName.set("admin");
        loginView.state.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);

        var productView = ProductViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertNotNull("A product should have been selected", productView.state.product.get());
        Assert.assertEquals("The product must have id==" + DBReset.FLASH_DRIVE_2GB_ID,
                (long) DBReset.FLASH_DRIVE_2GB_ID, (long) productView.state.product.get().id);

        productView.state.onAddToCart.run(1);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        var cartView = CartViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("There must be no error indication", 0, (int) cartView.state.errorCode.get());
        Assert.assertEquals("One item in the cart", 1, cartView.state.rows.get().size());
        var flashDrive = cartView.state.rows.get().get(0);
        Assert.assertEquals("The item must have quantity 1", 1, (int) flashDrive.quantity.get());
        Assert.assertEquals("ID in the cart <> " + DBReset.FLASH_DRIVE_2GB_ID,
                (long) DBReset.FLASH_DRIVE_2GB_ID, (long) flashDrive.id.get());

        flashDrive.onModifyQuantity.run(0);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("Invalid quantity indication", 1, (int) cartView.state.errorCode.get());
        cartView.state.errorCode.set(0);

        flashDrive.onModifyQuantity.run(2);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("It must work without errors", 0, (int) cartView.state.errorCode.get());
        Assert.assertEquals("The item must have quantity 2", 2, (int) cartView.state.rows.get().get(0).quantity.get());

        cartView.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.WILSON_BALL_ID);
        productView = ProductViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("Product WILSON_BALL not found", (long) DBReset.WILSON_BALL_ID, (long) productView.state.product.get().id);

        productView.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.THREAD_SEAL_TAPE_ID);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        productView = ProductViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("Product THREAD_SEAL_TAPE not found",
                (long) DBReset.THREAD_SEAL_TAPE_ID, (long) productView.state.product.get().id);

        productView.state.onAddToCart.run(1);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertEquals("Two items in the cart", 2, cartView.state.rows.get().size());
        Assert.assertEquals("The item must have quantity 1", 1, (int) cartView.state.rows.get().get(1).quantity.get());

        cartView.state.onBuy.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        var receiptView = ReceiptViewMock.cast(this.app.viewOf(homeView.state.content.get()));
        Assert.assertTrue("It must be flagged as a new receipt", receiptView.state.notifySuccess.get());
        Assert.assertNotNull(receiptView.state.receipt.get());
        Assert.assertEquals(2, receiptView.state.receipt.get().items.size());

        receiptView.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        Assert.assertNull("The restricted view should be showing the default content",
                homeView.state.content.get());
    }


    @Test
    public void cartRowsAreSegmentsWithTheirOwnActions() {
        new LoginKeys(this.app).flip();
        var rootView = this.app.getRootView();
        LoginViewMock.cast(this.app.viewOf(rootView.state.content.get())).state.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        homeView.getProductsPanelState().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);
        ProductViewMock.cast(this.app.viewOf(homeView.state.content.get())).state.onAddToCart.run(1);
        homeView.getProductsPanelState().onOpenProduct.run(DBReset.WILSON_BALL_ID);
        ProductViewMock.cast(this.app.viewOf(homeView.state.content.get())).state.onAddToCart.run(2);

        var cart = CartViewMock.cast(this.app.viewOf(homeView.state.content.get())).state;
        Assert.assertEquals("One row per item", 2, cart.rows.get().size());
        var flashDrive = cart.rows.get().get(0);
        Assert.assertEquals((long) DBReset.FLASH_DRIVE_2GB_ID, (long) flashDrive.id.get());
        var flashDriveView = app.viewOf(flashDrive);
        Assert.assertNotNull("Each row has its own view", flashDriveView);
        Assert.assertSame("Asked for again, it is the same view", flashDriveView, app.viewOf(flashDrive));
        Assert.assertEquals(16.00 + 2 * 45.30, cart.total.get(), 0.001);

        // The action belongs to the row: it does not carry the product id
        flashDrive.onModifyQuantity.run(3);
        Assert.assertEquals(3, (int) flashDrive.quantity.get());
        Assert.assertSame("The row is reused while the item stays", flashDrive, cart.rows.get().get(0));
        Assert.assertEquals(3 * 16.00 + 2 * 45.30, cart.total.get(), 0.001);

        flashDrive.onModifyQuantity.run(0);
        Assert.assertEquals("Invalid quantity", 1, (int) cart.errorCode.get());
        Assert.assertEquals(3, (int) flashDrive.quantity.get());

        flashDrive.onRemove.run();
        Assert.assertEquals(1, cart.rows.get().size());
        Assert.assertEquals((long) DBReset.WILSON_BALL_ID, (long) cart.rows.get().get(0).id.get());

        cart.rows.get().get(0).onRemove.run();
        Assert.assertNull("An empty cart goes back to the home", homeView.state.content.get());
    }
}
