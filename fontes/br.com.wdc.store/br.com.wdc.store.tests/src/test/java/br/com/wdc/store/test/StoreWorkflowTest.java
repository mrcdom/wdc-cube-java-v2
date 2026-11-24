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

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        loginView.scope.userName.set("admin");
        loginView.scope.onEnter.run("admin", "admin");

        return HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
    }

    @Test
    public void testViewNonexistentProduct() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        // A product that does not exist
        restrictedView.getProductsPanelScope().onOpenProduct.run(Long.MIN_VALUE);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        Assert.assertEquals("A negative id is not a product: " + restrictedView.scope.errorCode.get(),
                6, (int) restrictedView.scope.errorCode.get());

        restrictedView.getProductsPanelScope().onOpenProduct.run(999_999L);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        Assert.assertEquals("The error code must indicate that the product does not exist: " + restrictedView.scope.errorCode.get(),
                3, (int) restrictedView.scope.errorCode.get());
    }

    @Test
    public void testViewProduct() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        restrictedView.getProductsPanelScope().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);
        restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));

        var productView = ProductViewMock.cast(this.app.viewOf(restrictedView.scope.content.get()));
        Assert.assertNotNull("A product should have been selected", productView.scope.product.get());
    }

    @Test
    public void testBuyProduct() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        loginView.scope.userName.set("admin");
        loginView.scope.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));

        homeView.getProductsPanelScope().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);

        var productView = ProductViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertNotNull("A product should have been selected", productView.scope.product.get());
        Assert.assertEquals("The product must have id==" + DBReset.FLASH_DRIVE_2GB_ID,
                (long) DBReset.FLASH_DRIVE_2GB_ID, (long) productView.scope.product.get().id);

        productView.scope.onAddToCart.run(1);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        var cartView = CartViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("There must be no error indication", 0, (int) cartView.scope.errorCode.get());
        Assert.assertEquals("One item in the cart", 1, cartView.scope.rows.get().size());
        var flashDrive = cartView.scope.rows.get().get(0);
        Assert.assertEquals("The item must have quantity 1", 1, (int) flashDrive.quantity.get());
        Assert.assertEquals("ID in the cart <> " + DBReset.FLASH_DRIVE_2GB_ID,
                (long) DBReset.FLASH_DRIVE_2GB_ID, (long) flashDrive.id.get());

        flashDrive.onModifyQuantity.run(0);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("Invalid quantity indication", 1, (int) cartView.scope.errorCode.get());
        cartView.scope.errorCode.set(0);

        flashDrive.onModifyQuantity.run(2);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("It must work without errors", 0, (int) cartView.scope.errorCode.get());
        Assert.assertEquals("The item must have quantity 2", 2, (int) cartView.scope.rows.get().get(0).quantity.get());

        cartView.scope.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));

        homeView.getProductsPanelScope().onOpenProduct.run(DBReset.WILSON_BALL_ID);
        productView = ProductViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("Product WILSON_BALL not found", (long) DBReset.WILSON_BALL_ID, (long) productView.scope.product.get().id);

        productView.scope.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));

        homeView.getProductsPanelScope().onOpenProduct.run(DBReset.THREAD_SEAL_TAPE_ID);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        productView = ProductViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("Product THREAD_SEAL_TAPE not found",
                (long) DBReset.THREAD_SEAL_TAPE_ID, (long) productView.scope.product.get().id);

        productView.scope.onAddToCart.run(1);
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        cartView = CartViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertEquals("Two items in the cart", 2, cartView.scope.rows.get().size());
        Assert.assertEquals("The item must have quantity 1", 1, (int) cartView.scope.rows.get().get(1).quantity.get());

        cartView.scope.onBuy.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        var receiptView = ReceiptViewMock.cast(this.app.viewOf(homeView.scope.content.get()));
        Assert.assertTrue("It must be flagged as a new receipt", receiptView.scope.notifySuccess.get());
        Assert.assertNotNull(receiptView.scope.receipt.get());
        Assert.assertEquals(2, receiptView.scope.receipt.get().items.size());

        receiptView.scope.onOpenProducts.run();
        homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        Assert.assertNull("The restricted view should be showing the default content",
                homeView.scope.content.get());
    }


    @Test
    public void cartRowsAreSegmentsWithTheirOwnActions() {
        new LoginKeys(this.app).flip();
        var rootView = this.app.getRootView();
        LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get())).scope.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        homeView.getProductsPanelScope().onOpenProduct.run(DBReset.FLASH_DRIVE_2GB_ID);
        ProductViewMock.cast(this.app.viewOf(homeView.scope.content.get())).scope.onAddToCart.run(1);
        homeView.getProductsPanelScope().onOpenProduct.run(DBReset.WILSON_BALL_ID);
        ProductViewMock.cast(this.app.viewOf(homeView.scope.content.get())).scope.onAddToCart.run(2);

        var cart = CartViewMock.cast(this.app.viewOf(homeView.scope.content.get())).scope;
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
        Assert.assertNull("An empty cart goes back to the home", homeView.scope.content.get());
    }
}
