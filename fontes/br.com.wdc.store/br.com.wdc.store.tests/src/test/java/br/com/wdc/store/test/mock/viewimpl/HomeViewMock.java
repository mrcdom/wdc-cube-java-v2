package br.com.wdc.store.test.mock.viewimpl;

import java.util.Date;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.PurchasesPanelScope;
import br.com.wdc.store.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.store.test.mock.StoreApplicationMock;

@SuppressWarnings("java:S106")
public class HomeViewMock extends AbstractViewMock<HomeScope> {

    public static HomeViewMock cast(CubeView view) {
        var cls = HomeViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (HomeViewMock) view;
    }

    public HomeViewMock(StoreApplicationMock app, HomeScope state) {
        super(app, state);
    }

    public ProductsPanelScope getProductsPanelState() {
        return ProductsPanelViewMock.cast(this.app.viewOf(this.state.productsPanel.get())).state;
    }

    public PurchasesPanelScope getPurchasesPanelState() {
        return PurchasesPanelViewMock.cast(this.app.viewOf(this.state.purchasesPanel.get())).state;
    }

    public void render() {
        System.out.println("Welcome, " + this.state.nickName.get() + "!");

        System.out.println();

        System.out.println("Cart[" + this.state.cartItemCount.get() + "]");
        System.out.println();

        this.printPurchases();
        this.printProducts();

        System.out.println("---------------------------------------------------------");
    }

    public void printPurchases() {
        var purchasesState = this.getPurchasesPanelState();
        for (final PurchaseInfo purchase : purchasesState.purchases.get()) {
            System.out.println("PURCHASE #" + purchase.id);
            System.out.println("Purchase date: " + new Date(purchase.date));
            System.out.println("Items purchased: " + purchase.items);
            System.out.println("Total: R$ " + purchase.total);
            System.out.println();
        }

    }

    public void printProducts() {
        var productsState = this.getProductsPanelState();
        for (final ProductInfo product : productsState.products.get()) {
            System.out.print("PRODUCT #" + product.id);
            System.out.print("{name: ");
            System.out.print(product.name);
            System.out.print(", price: ");
            System.out.print(product.price);
            System.out.print(", image: ");
            System.out.print(product.image);
            System.out.println("}");
        }
    }

}
