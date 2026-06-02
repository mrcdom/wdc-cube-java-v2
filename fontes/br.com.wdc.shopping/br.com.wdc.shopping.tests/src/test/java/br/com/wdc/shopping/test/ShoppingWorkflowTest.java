package br.com.wdc.shopping.test;

import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.mock.viewimpl.CartViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

public class ShoppingWorkflowTest extends BasePresentationTest {

    private HomeViewMock gotoRestricted() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.presenter.onEnter("admin", "admin");

        return HomeViewMock.cast(rootView.state.contentView);
    }

    @Test
    public void testVisualizaProdutoInexistente() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        // Produto que não existe
        restrictedView.presenter.onOpenProduct(Long.MIN_VALUE);
        restrictedView = HomeViewMock.cast(rootView.state.contentView);
        Assert.assertEquals("O código de erro deve estar indicado produto não existe: " + restrictedView.state.errorCode,
                3, restrictedView.state.errorCode);
    }

    @Test
    public void testVisualizaProduto() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        restrictedView.presenter.onOpenProduct(DBReset.PEN_DRIVE2GB_ID);
        restrictedView = HomeViewMock.cast(rootView.state.contentView);

        var produtoView = ProductViewMock.cast(restrictedView.state.contentView);
        Assert.assertNotNull("Produto deve ter sido selecionado", produtoView.state.product);
    }

    @Test
    public void testComprarProduto() {
        Routes.login(this.app);

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView);
        loginView.state.userName = "admin";
        loginView.presenter.onEnter("admin", "admin");

        var homeView = HomeViewMock.cast(rootView.state.contentView);

        homeView.presenter.onOpenProduct(DBReset.PEN_DRIVE2GB_ID);

        var produtoView = ProductViewMock.cast(homeView.state.contentView);
        Assert.assertNotNull("Produto deve ter sido selecionado", produtoView.state.product);
        Assert.assertEquals("Produto deve ser o id==" + DBReset.PEN_DRIVE2GB_ID,
                (long) DBReset.PEN_DRIVE2GB_ID, (long) produtoView.state.product.id);

        produtoView.presenter.onAddToCart(1);
        homeView = HomeViewMock.cast(rootView.state.contentView);
        var carrinhoView = CartViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Não deve haver indicação de erros", 0, carrinhoView.state.errorCode);
        Assert.assertEquals("Um item no carrinho", 1, carrinhoView.state.items.size());
        Assert.assertEquals("O item deve ter quantidade 1", 1, carrinhoView.state.items.get(0).quantity);
        Assert.assertEquals("ID no carriho <> " + DBReset.PEN_DRIVE2GB_ID,
                (long) DBReset.PEN_DRIVE2GB_ID, carrinhoView.state.items.get(0).id);

        carrinhoView.presenter.onModifyQuantity(DBReset.PEN_DRIVE2GB_ID, 0);
        homeView = HomeViewMock.cast(rootView.state.contentView);
        carrinhoView = CartViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Indicação de quantidade inválida", 1, carrinhoView.state.errorCode);
        carrinhoView.state.errorCode = 0;

        carrinhoView.presenter.onModifyQuantity(DBReset.PEN_DRIVE2GB_ID, 2);
        homeView = HomeViewMock.cast(rootView.state.contentView);
        carrinhoView = CartViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Tem que funcionar sem erro", 0, carrinhoView.state.errorCode);
        Assert.assertEquals("O item deve ter quantidade 2", 2, carrinhoView.state.items.get(0).quantity);

        carrinhoView.presenter.onModifyQuantity(Long.MIN_VALUE, 2);
        carrinhoView = CartViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Produto não encontrado", 2, carrinhoView.state.errorCode);
        carrinhoView.state.errorCode = 0;

        carrinhoView.presenter.onOpenProducts();
        homeView = HomeViewMock.cast(rootView.state.contentView);

        homeView.presenter.onOpenProduct(DBReset.BOLA_WILSON_ID);
        produtoView = ProductViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Produto BOLA_WILSON não localizado", (long) DBReset.BOLA_WILSON_ID, (long) produtoView.state.product.id);

        produtoView.presenter.onOpenProducts();
        homeView = HomeViewMock.cast(rootView.state.contentView);

        homeView.presenter.onOpenProduct(DBReset.FITA_VEDA_ROSCA_ID);
        homeView = HomeViewMock.cast(rootView.state.contentView);
        produtoView = ProductViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Produto FITA_VEDA_ROSCA não localizado",
                (long) DBReset.FITA_VEDA_ROSCA_ID, (long) produtoView.state.product.id);

        produtoView.presenter.onAddToCart(1);
        homeView = HomeViewMock.cast(rootView.state.contentView);
        carrinhoView = CartViewMock.cast(homeView.state.contentView);
        Assert.assertEquals("Um item no carrinho", 2, carrinhoView.state.items.size());
        Assert.assertEquals("O item deve ter quantidade 1", 1, carrinhoView.state.items.get(1).quantity);

        carrinhoView.presenter.onBuy();
        homeView = HomeViewMock.cast(rootView.state.contentView);
        var reciboView = ReceiptViewMock.cast(homeView.state.contentView);
        Assert.assertTrue("Tem que estar marcado como novo recibo", reciboView.state.notifySuccess);
        Assert.assertNotNull(reciboView.state.receipt);
        Assert.assertEquals(2, reciboView.state.receipt.items.size());

        reciboView.presenter.onOpenProducts();
        homeView = HomeViewMock.cast(rootView.state.contentView);
        Assert.assertNull("A visão restrita deveria estar mostrando o conteúdo padrão",
                homeView.state.contentView);
    }

}
