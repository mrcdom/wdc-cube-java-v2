package br.com.wdc.store.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.store.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.store.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.store.test.util.BasePresentationTest;

@SuppressWarnings({ "java:S2068", "java:S1192", "java:S5961" })
public class StoreLoginTest extends BasePresentationTest {

    @Test
    public void testLoginFirstAccess() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var mainContent = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        Assert.assertEquals("The user should not have been validated", 0, (int) mainContent.scope.errorCode.get());
    }

    @Test
    public void testLoginFailsForUnrecognizedPasswordOrUser() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        loginView.scope.userName.set("admin");
        loginView.scope.onEnter.run("admin", "unrecognized password");

        // Check if it keeps bean login view
        loginView = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        Assert.assertEquals("The user should not have been validated", 1, (int) loginView.scope.errorCode.get());
    }

    @Test
    public void testLoginAccessToTheSystem() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.scope.content.get()));
        loginView.scope.userName.set("admin");
        loginView.scope.onEnter.run("admin", "admin");

        var restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.scope.content.get()));

        Assert.assertTrue("Invalid user name", StringUtils.isNotBlank(restrictedView.scope.nickName.get()));
        Assert.assertTrue("The number of items in the cart cannot be negative",
                (int) restrictedView.scope.cartItemCount.get() >= 0);
        Assert.assertEquals("The user should have been validated", 0, (int) restrictedView.scope.errorCode.get());

        var purchasesScope = restrictedView.getPurchasesPanelScope();
        var productsScope = restrictedView.getProductsPanelScope();

        Assert.assertNotNull("Missing purchase list", purchasesScope.purchases.get());
        Assert.assertNotNull("Missing product list", productsScope.products.get());

        // Simulate the view reporting capacity (no real view in tests)
        purchasesScope.onItemSizeCapacityChanged.run(3);

        Assert.assertEquals("John Smith", restrictedView.scope.nickName.get());
        Assert.assertEquals(0, (int) restrictedView.scope.cartItemCount.get());

        // Pagination metadata
        Assert.assertEquals(0, (int) purchasesScope.page.get());
        Assert.assertEquals(3, (int) purchasesScope.pageSize.get());
        Assert.assertEquals(2, (int) purchasesScope.totalCount.get());

        Assert.assertEquals(2, purchasesScope.purchases.get().size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(purchasesScope.purchases.get().get(0).id));
        Assert.assertEquals(47.97, purchasesScope.purchases.get().get(0).total, 0.001);
        Assert.assertEquals(2, purchasesScope.purchases.get().get(0).items.size());
        Assert.assertEquals("Wilson Ball", purchasesScope.purchases.get().get(0).items.get(0));
        Assert.assertEquals("Thread Seal Tape", purchasesScope.purchases.get().get(0).items.get(1));

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(purchasesScope.purchases.get().get(1).id));
        Assert.assertEquals(200.0, purchasesScope.purchases.get().get(1).total, 0.001);
        Assert.assertEquals(1, purchasesScope.purchases.get().get(1).items.size());
        Assert.assertEquals("Italian Design Coffee Maker", purchasesScope.purchases.get().get(1).items.get(0));

        Assert.assertEquals(4, productsScope.products.get().size());

        Assert.assertEquals(DBReset.COFFEE_MAKER_ID, Long.valueOf(productsScope.products.get().get(0).id));
        Assert.assertEquals("unknown", productsScope.products.get().get(0).description);

        Assert.assertEquals(DBReset.WILSON_BALL_ID, Long.valueOf(productsScope.products.get().get(1).id));
        Assert.assertEquals("unknown", productsScope.products.get().get(1).description);

        Assert.assertEquals(DBReset.THREAD_SEAL_TAPE_ID, Long.valueOf(productsScope.products.get().get(2).id));
        Assert.assertEquals("unknown", productsScope.products.get().get(2).description);

        Assert.assertEquals(DBReset.FLASH_DRIVE_2GB_ID, Long.valueOf(productsScope.products.get().get(3).id));
        Assert.assertEquals("unknown", productsScope.products.get().get(3).description);
    }

}
