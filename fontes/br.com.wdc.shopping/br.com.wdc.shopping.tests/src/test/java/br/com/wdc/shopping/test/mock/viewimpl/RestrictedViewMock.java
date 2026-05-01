package br.com.wdc.shopping.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings("java:S106")
public class RestrictedViewMock extends AbstractViewMock<HomePresenter> {

    public static RestrictedViewMock cast(CubeView view) {
        var cls = RestrictedViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (RestrictedViewMock) view;
    }

    public HomeViewState state;

    public RestrictedViewMock(ShoppingApplicationMock app, HomePresenter presenter) {
        super(app, presenter);
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
