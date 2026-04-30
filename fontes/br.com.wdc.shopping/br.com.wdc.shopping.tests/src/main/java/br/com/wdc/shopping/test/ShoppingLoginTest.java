package br.com.wdc.shopping.test;

import org.junit.Assert;
import org.junit.Test;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.shopping.persistence.sgbd.ddl.scripts.DBReset;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RestrictedViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

@SuppressWarnings({"java:S2068", "java:S1192"})
public class ShoppingLoginTest extends BasePresentationTest {

    @Test
    public void testLoginPrimeiroAcesso() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var mainContent = LoginViewMock.cast(rootView.state.contentView);
        Assert.assertTrue("Usuário não poderia ter sido validado", mainContent.state.errorCode == 0);
    }

    @Test
    public void testLoginFalhaPorSenhaOuUsuarioNaoReconhecidos() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.state.password = "senha não reconhecida";
        loginView.presenter.onEnter();

        // Check if it keeps bean login view
        loginView = LoginViewMock.cast(rootView.state.contentView);
        Assert.assertTrue("Usuário não poderia ter sido validado", loginView.state.errorCode == 1);
    }

    @Test
    public void testLoginAcessoAoSistema() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.state.password = "admin";
        loginView.presenter.onEnter();

        var restrictedView = RestrictedViewMock.cast(rootView.state.contentView);

        Assert.assertTrue("Nome do usuário inválido", StringUtils.isNotBlank(restrictedView.state.nickName));
        Assert.assertTrue("Quantidade itens no carrinho não pode ser negativo",
                restrictedView.state.cartItemCount >= 0);
        Assert.assertTrue("Usuário deveria ter sido validado", restrictedView.state.errorCode == 0);

        var purchasesState = restrictedView.getPurchasesPanelState();
        var productsState = restrictedView.getProductsPanelState();

        Assert.assertNotNull("Falta lista de compras", purchasesState.purchases);
        Assert.assertNotNull("Falta lista de produtos", productsState.products);

        Assert.assertEquals("João da Silva", restrictedView.state.nickName);
        Assert.assertEquals(0, restrictedView.state.cartItemCount);

        // Pagination metadata
        Assert.assertEquals(0, purchasesState.page);
        Assert.assertEquals(2, purchasesState.pageSize);
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
        Assert.assertNull(productsState.products.get(0).description);

        Assert.assertEquals(DBReset.BOLA_WILSON_ID, Long.valueOf(productsState.products.get(1).id));
        Assert.assertNull(productsState.products.get(1).description);

        Assert.assertEquals(DBReset.FITA_VEDA_ROSCA_ID, Long.valueOf(productsState.products.get(2).id));
        Assert.assertNull(productsState.products.get(2).description);

        Assert.assertEquals(DBReset.PEN_DRIVE2GB_ID, Long.valueOf(productsState.products.get(3).id));
        Assert.assertNull(productsState.products.get(3).description);
    }

}
