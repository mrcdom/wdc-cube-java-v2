package br.com.wdc.shopping.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

@SuppressWarnings({ "java:S2068", "java:S1192", "java:S5961" })
public class ShoppingLoginTest extends BasePresentationTest {

    @Test
    public void testLoginPrimeiroAcesso() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var mainContent = LoginViewMock.cast(rootView.state.contentView);
        Assert.assertEquals("Usuário não poderia ter sido validado", 0, mainContent.state.errorCode);
    }

    @Test
    public void testLoginFalhaPorSenhaOuUsuarioNaoReconhecidos() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.presenter.onEnter("admin", "senha não reconhecida");

        // Check if it keeps bean login view
        loginView = LoginViewMock.cast(rootView.state.contentView);
        Assert.assertEquals("Usuário não poderia ter sido validado", 1, loginView.state.errorCode);
    }

    @Test
    public void testLoginAcessoAoSistema() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.presenter.onEnter("admin", "admin");

        var restrictedView = HomeViewMock.cast(rootView.state.contentView);

        Assert.assertTrue("Nome do usuário inválido", StringUtils.isNotBlank(restrictedView.state.nickName));
        Assert.assertTrue("Quantidade itens no carrinho não pode ser negativo",
                restrictedView.state.cartItemCount >= 0);
        Assert.assertEquals("Usuário deveria ter sido validado", 0, restrictedView.state.errorCode);

        var purchasesState = restrictedView.getPurchasesPanelState();
        var productsState = restrictedView.getProductsPanelState();

        Assert.assertNotNull("Falta lista de compras", purchasesState.purchases);
        Assert.assertNotNull("Falta lista de produtos", productsState.products);

        // Simulate the view reporting capacity (no real view in tests)
        var purchasesMock = br.com.wdc.shopping.test.mock.viewimpl.PurchasesPanelViewMock
                .cast(restrictedView.state.purchasesPanelView);
        purchasesMock.presenter.onItemSizeCapacityChanged(3);

        Assert.assertEquals("João da Silva", restrictedView.state.nickName);
        Assert.assertEquals(0, restrictedView.state.cartItemCount);

        // Pagination metadata
        Assert.assertEquals(0, purchasesState.page);
        Assert.assertEquals(3, purchasesState.pageSize);
        Assert.assertEquals(2, purchasesState.totalCount);

        Assert.assertEquals(2, purchasesState.purchases.size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(purchasesState.purchases.get(0).id));
        Assert.assertEquals(47.97, purchasesState.purchases.get(0).total, 0.001);
        Assert.assertEquals(2, purchasesState.purchases.get(0).items.size());
        Assert.assertEquals("Bola Wilson", purchasesState.purchases.get(0).items.get(0));
        Assert.assertEquals("Fita veda rosca", purchasesState.purchases.get(0).items.get(1));

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(purchasesState.purchases.get(1).id));
        Assert.assertEquals(200.0, purchasesState.purchases.get(1).total, 0.001);
        Assert.assertEquals(1, purchasesState.purchases.get(1).items.size());
        Assert.assertEquals("Cafeteira design italiano", purchasesState.purchases.get(1).items.get(0));

        Assert.assertEquals(4, productsState.products.size());

        Assert.assertEquals(DBReset.CAFETEIRA_ID, Long.valueOf(productsState.products.get(0).id));
        Assert.assertEquals("unknown", productsState.products.get(0).description);

        Assert.assertEquals(DBReset.BOLA_WILSON_ID, Long.valueOf(productsState.products.get(1).id));
        Assert.assertEquals("unknown", productsState.products.get(1).description);

        Assert.assertEquals(DBReset.FITA_VEDA_ROSCA_ID, Long.valueOf(productsState.products.get(2).id));
        Assert.assertEquals("unknown", productsState.products.get(2).description);

        Assert.assertEquals(DBReset.PEN_DRIVE2GB_ID, Long.valueOf(productsState.products.get(3).id));
        Assert.assertEquals("unknown", productsState.products.get(3).description);
    }

}
