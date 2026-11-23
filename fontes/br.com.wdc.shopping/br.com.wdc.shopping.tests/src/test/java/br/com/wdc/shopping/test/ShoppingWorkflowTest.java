package br.com.wdc.shopping.test;

import org.junit.Assert;
import org.junit.Test;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.mock.viewimpl.CartViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.shopping.test.util.BasePresentationTest;

public class ShoppingWorkflowTest extends BasePresentationTest {

    private HomeViewMock gotoRestricted() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView.get());
        loginView.state.userName.set("admin");
        loginView.presenter.state.onEnter.run("admin", "admin");

        return HomeViewMock.cast(rootView.state.contentView.get());
    }

    @Test
    public void testVisualizaProdutoInexistente() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        // Produto que não existe
        restrictedView.getProductsPanelState().onOpenProduct.run(Long.MIN_VALUE);
        restrictedView = HomeViewMock.cast(rootView.state.contentView.get());
        Assert.assertEquals("Um id negativo não é um produto: " + restrictedView.state.errorCode.get(),
                6, (int) restrictedView.state.errorCode.get());

        restrictedView.getProductsPanelState().onOpenProduct.run(999_999L);
        restrictedView = HomeViewMock.cast(rootView.state.contentView.get());
        Assert.assertEquals("O código de erro deve estar indicado produto não existe: " + restrictedView.state.errorCode.get(),
                3, (int) restrictedView.state.errorCode.get());
    }

    @Test
    public void testVisualizaProduto() {
        var restrictedView = gotoRestricted();
        var rootView = this.app.getRootView();

        restrictedView.getProductsPanelState().onOpenProduct.run(DBReset.PEN_DRIVE2GB_ID);
        restrictedView = HomeViewMock.cast(rootView.state.contentView.get());

        var produtoView = ProductViewMock.cast(restrictedView.state.contentView.get());
        Assert.assertNotNull("Produto deve ter sido selecionado", produtoView.state.product.get());
    }

    @Test
    public void testComprarProduto() {
        new LoginKeys(this.app).flip();

        var rootView = this.app.getRootView();

        var loginView = LoginViewMock.cast(rootView.state.contentView.get());
        loginView.state.userName.set("admin");
        loginView.presenter.state.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(rootView.state.contentView.get());

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.PEN_DRIVE2GB_ID);

        var produtoView = ProductViewMock.cast(homeView.state.contentView.get());
        Assert.assertNotNull("Produto deve ter sido selecionado", produtoView.state.product.get());
        Assert.assertEquals("Produto deve ser o id==" + DBReset.PEN_DRIVE2GB_ID,
                (long) DBReset.PEN_DRIVE2GB_ID, (long) produtoView.state.product.get().id);

        produtoView.presenter.state.onAddToCart.run(1);
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        var carrinhoView = CartViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Não deve haver indicação de erros", 0, (int) carrinhoView.state.errorCode.get());
        Assert.assertEquals("Um item no carrinho", 1, carrinhoView.state.items.get().size());
        Assert.assertEquals("O item deve ter quantidade 1", 1, carrinhoView.state.items.get().get(0).quantity);
        Assert.assertEquals("ID no carriho <> " + DBReset.PEN_DRIVE2GB_ID,
                (long) DBReset.PEN_DRIVE2GB_ID, carrinhoView.state.items.get().get(0).id);

        carrinhoView.presenter.state.onModifyQuantity.run(DBReset.PEN_DRIVE2GB_ID, 0);
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        carrinhoView = CartViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Indicação de quantidade inválida", 1, (int) carrinhoView.state.errorCode.get());
        carrinhoView.state.errorCode.set(0);

        carrinhoView.presenter.state.onModifyQuantity.run(DBReset.PEN_DRIVE2GB_ID, 2);
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        carrinhoView = CartViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Tem que funcionar sem erro", 0, (int) carrinhoView.state.errorCode.get());
        Assert.assertEquals("O item deve ter quantidade 2", 2, carrinhoView.state.items.get().get(0).quantity);

        carrinhoView.presenter.state.onModifyQuantity.run(Long.MIN_VALUE, 2);
        carrinhoView = CartViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Produto não encontrado", 2, (int) carrinhoView.state.errorCode.get());
        carrinhoView.state.errorCode.set(0);

        carrinhoView.presenter.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(rootView.state.contentView.get());

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.BOLA_WILSON_ID);
        produtoView = ProductViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Produto BOLA_WILSON não localizado", (long) DBReset.BOLA_WILSON_ID, (long) produtoView.state.product.get().id);

        produtoView.presenter.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(rootView.state.contentView.get());

        homeView.getProductsPanelState().onOpenProduct.run(DBReset.FITA_VEDA_ROSCA_ID);
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        produtoView = ProductViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Produto FITA_VEDA_ROSCA não localizado",
                (long) DBReset.FITA_VEDA_ROSCA_ID, (long) produtoView.state.product.get().id);

        produtoView.presenter.state.onAddToCart.run(1);
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        carrinhoView = CartViewMock.cast(homeView.state.contentView.get());
        Assert.assertEquals("Um item no carrinho", 2, carrinhoView.state.items.get().size());
        Assert.assertEquals("O item deve ter quantidade 1", 1, carrinhoView.state.items.get().get(1).quantity);

        carrinhoView.presenter.state.onBuy.run();
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        var reciboView = ReceiptViewMock.cast(homeView.state.contentView.get());
        Assert.assertTrue("Tem que estar marcado como novo recibo", reciboView.state.notifySuccess.get());
        Assert.assertNotNull(reciboView.state.receipt.get());
        Assert.assertEquals(2, reciboView.state.receipt.get().items.size());

        reciboView.presenter.state.onOpenProducts.run();
        homeView = HomeViewMock.cast(rootView.state.contentView.get());
        Assert.assertNull("A visão restrita deveria estar mostrando o conteúdo padrão",
                homeView.state.contentView.get());
    }


    @Test
    public void cartRowsAreSegmentsWithTheirOwnActions() {
        new LoginKeys(this.app).flip();
        var rootView = this.app.getRootView();
        LoginViewMock.cast(rootView.state.contentView.get()).state.onEnter.run("admin", "admin");

        var homeView = HomeViewMock.cast(rootView.state.contentView.get());
        homeView.getProductsPanelState().onOpenProduct.run(DBReset.PEN_DRIVE2GB_ID);
        ProductViewMock.cast(homeView.state.contentView.get()).state.onAddToCart.run(1);
        homeView.getProductsPanelState().onOpenProduct.run(DBReset.BOLA_WILSON_ID);
        ProductViewMock.cast(homeView.state.contentView.get()).state.onAddToCart.run(2);

        var cart = CartViewMock.cast(homeView.state.contentView.get()).state;
        Assert.assertEquals("Uma linha por item", 2, cart.rows.get().size());
        var penDrive = cart.rows.get().get(0);
        Assert.assertEquals((long) DBReset.PEN_DRIVE2GB_ID, (long) penDrive.id.get());
        Assert.assertNotNull("Cada linha tem a sua view", penDrive.view());
        Assert.assertEquals(16.00 + 2 * 45.30, cart.total.get(), 0.001);

        // A ação é da linha: não leva o id do produto
        penDrive.onModifyQuantity.run(3);
        Assert.assertEquals(3, (int) penDrive.quantity.get());
        Assert.assertSame("A linha é reaproveitada enquanto o item fica", penDrive, cart.rows.get().get(0));
        Assert.assertEquals(3 * 16.00 + 2 * 45.30, cart.total.get(), 0.001);

        penDrive.onModifyQuantity.run(0);
        Assert.assertEquals("Quantidade inválida", 1, (int) cart.errorCode.get());
        Assert.assertEquals(3, (int) penDrive.quantity.get());

        penDrive.onRemove.run();
        Assert.assertEquals(1, cart.rows.get().size());
        Assert.assertEquals((long) DBReset.BOLA_WILSON_ID, (long) cart.rows.get().get(0).id.get());

        cart.rows.get().get(0).onRemove.run();
        Assert.assertNull("Carrinho vazio volta para a home", homeView.state.contentView.get());
    }
}
