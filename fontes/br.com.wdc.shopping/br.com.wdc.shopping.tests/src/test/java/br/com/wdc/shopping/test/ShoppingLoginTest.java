package br.com.wdc.shopping.test;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

@SuppressWarnings({ "java:S2068", "java:S1192", "java:S5961" })
public class ShoppingLoginTest extends BasePresentationTest {

    @Test
    public void testLoginPrimeiroAcesso() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var mainContent = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        Assert.assertEquals("Usuário não poderia ter sido validado", 0, (int) mainContent.state.errorCode.get());
    }

    @Test
    public void testLoginFalhaPorSenhaOuUsuarioNaoReconhecidos() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        loginView.state.userName.set("admin");
        loginView.state.onEnter.run("admin", "senha não reconhecida");

        // Check if it keeps bean login view
        loginView = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        Assert.assertEquals("Usuário não poderia ter sido validado", 1, (int) loginView.state.errorCode.get());
    }

    @Test
    public void testLoginAcessoAoSistema() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(this.app.viewOf(rootView.state.content.get()));
        loginView.state.userName.set("admin");
        loginView.state.onEnter.run("admin", "admin");

        var restrictedView = HomeViewMock.cast(this.app.viewOf(rootView.state.content.get()));

        Assert.assertTrue("Nome do usuário inválido", StringUtils.isNotBlank(restrictedView.state.nickName.get()));
        Assert.assertTrue("Quantidade itens no carrinho não pode ser negativo",
                (int) restrictedView.state.cartItemCount.get() >= 0);
        Assert.assertEquals("Usuário deveria ter sido validado", 0, (int) restrictedView.state.errorCode.get());

        var purchasesState = restrictedView.getPurchasesPanelState();
        var productsState = restrictedView.getProductsPanelState();

        Assert.assertNotNull("Falta lista de compras", purchasesState.purchases.get());
        Assert.assertNotNull("Falta lista de produtos", productsState.products.get());

        // Simulate the view reporting capacity (no real view in tests)
        purchasesState.onItemSizeCapacityChanged.run(3);

        Assert.assertEquals("João da Silva", restrictedView.state.nickName.get());
        Assert.assertEquals(0, (int) restrictedView.state.cartItemCount.get());

        // Pagination metadata
        Assert.assertEquals(0, (int) purchasesState.page.get());
        Assert.assertEquals(3, (int) purchasesState.pageSize.get());
        Assert.assertEquals(2, (int) purchasesState.totalCount.get());

        Assert.assertEquals(2, purchasesState.purchases.get().size());

        Assert.assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, Long.valueOf(purchasesState.purchases.get().get(0).id));
        Assert.assertEquals(47.97, purchasesState.purchases.get().get(0).total, 0.001);
        Assert.assertEquals(2, purchasesState.purchases.get().get(0).items.size());
        Assert.assertEquals("Bola Wilson", purchasesState.purchases.get().get(0).items.get(0));
        Assert.assertEquals("Fita veda rosca", purchasesState.purchases.get().get(0).items.get(1));

        Assert.assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, Long.valueOf(purchasesState.purchases.get().get(1).id));
        Assert.assertEquals(200.0, purchasesState.purchases.get().get(1).total, 0.001);
        Assert.assertEquals(1, purchasesState.purchases.get().get(1).items.size());
        Assert.assertEquals("Cafeteira design italiano", purchasesState.purchases.get().get(1).items.get(0));

        Assert.assertEquals(4, productsState.products.get().size());

        Assert.assertEquals(DBReset.CAFETEIRA_ID, Long.valueOf(productsState.products.get().get(0).id));
        Assert.assertEquals("unknown", productsState.products.get().get(0).description);

        Assert.assertEquals(DBReset.BOLA_WILSON_ID, Long.valueOf(productsState.products.get().get(1).id));
        Assert.assertEquals("unknown", productsState.products.get().get(1).description);

        Assert.assertEquals(DBReset.FITA_VEDA_ROSCA_ID, Long.valueOf(productsState.products.get().get(2).id));
        Assert.assertEquals("unknown", productsState.products.get().get(2).description);

        Assert.assertEquals(DBReset.PEN_DRIVE2GB_ID, Long.valueOf(productsState.products.get().get(3).id));
        Assert.assertEquals("unknown", productsState.products.get().get(3).description);
    }

}
