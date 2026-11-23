package br.com.wdc.shopping.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope.ProductsPanelScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope.PurchasesPanelScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings("java:S106")
public class HomeViewMock extends AbstractViewMock<HomeScope> {

    public static HomeViewMock cast(CubeView view) {
        var cls = HomeViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (HomeViewMock) view;
    }


    public HomeViewMock(ShoppingApplicationMock app, HomeScope state) {
        super(app, state);
    }

    public ProductsPanelScope getProductsPanelState() {
        return ProductsPanelViewMock.cast(this.app.viewOf(this.state.productsPanel.get())).state;
    }

    public PurchasesPanelScope getPurchasesPanelState() {
        return PurchasesPanelViewMock.cast(this.app.viewOf(this.state.purchasesPanel.get())).state;
    }

    public void render() {
        System.out.println("Seja bem vindo, " + this.state.nickName.get() + "!");

        System.out.println();

        System.out.println("Carrinho[" + this.state.cartItemCount.get() + "]");
        System.out.println();

        this.printCompras();
        this.printProdutos();

        System.out.println("---------------------------------------------------------");
    }

    public void printCompras() {
        var purchasesState = this.getPurchasesPanelState();
        for (final PurchaseInfo compra : purchasesState.purchases.get()) {
            System.out.println("COMPRA #" + compra.id);
            System.out.println("Data da compra: " + new Date(compra.date));
            System.out.println("Itens adquiridos: " + compra.items);
            System.out.println("Valor total: R$ " + compra.total);
            System.out.println();
        }

    }

    public void printProdutos() {
        var productsState = this.getProductsPanelState();
        for (final ProductInfo produto : productsState.products.get()) {
            System.out.print("PRODUTO #" + produto.id);
            System.out.print("{nome: ");
            System.out.print(produto.name);
            System.out.print(", valor: ");
            System.out.print(produto.price);
            System.out.print(", imagem: ");
            System.out.print(produto.image);
            System.out.println("}");
        }
    }

}
