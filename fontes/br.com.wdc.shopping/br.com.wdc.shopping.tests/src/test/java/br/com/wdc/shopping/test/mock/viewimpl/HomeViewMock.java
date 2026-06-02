package br.com.wdc.shopping.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings("java:S106")
public class HomeViewMock extends AbstractViewMock<HomePresenter> {

    public static HomeViewMock cast(CubeView view) {
        var cls = HomeViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (HomeViewMock) view;
    }

    public HomeViewState state;

    public HomeViewMock(HomePresenter presenter) {
        super(((ShoppingApplicationMock) presenter.app), presenter);
        this.state = presenter.state;
    }

    public ProductsPanelViewState getProductsPanelState() {
        return ProductsPanelViewMock.cast(this.state.productsPanelView).state;
    }

    public PurchasesPanelViewState getPurchasesPanelState() {
        return PurchasesPanelViewMock.cast(this.state.purchasesPanelView).state;
    }

    public void render() {
        System.out.println("Seja bem vindo, " + this.state.nickName + "!");

        System.out.println();

        System.out.println("Carrinho[" + this.state.cartItemCount + "]");
        System.out.println();

        this.printCompras();
        this.printProdutos();

        System.out.println("---------------------------------------------------------");
    }

    public void printCompras() {
        var purchasesState = this.getPurchasesPanelState();
        for (final PurchaseInfo compra : purchasesState.purchases) {
            System.out.println("COMPRA #" + compra.id);
            System.out.println("Data da compra: " + new Date(compra.date));
            System.out.println("Itens adquiridos: " + compra.items);
            System.out.println("Valor total: R$ " + compra.total);
            System.out.println();
        }

    }

    public void printProdutos() {
        var productsState = this.getProductsPanelState();
        for (final ProductInfo produto : productsState.products) {
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
