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

    public HomeViewMock(StoreApplicationMock app, HomeScope scope) {
        super(app, scope);
    }

    public ProductsPanelScope getProductsPanelScope() {
        return ProductsPanelViewMock.cast(this.app.viewOf(this.scope.productsPanel.get())).scope;
    }

    public PurchasesPanelScope getPurchasesPanelScope() {
        return PurchasesPanelViewMock.cast(this.app.viewOf(this.scope.purchasesPanel.get())).scope;
    }

    public void render() {
        System.out.println("Welcome, " + this.scope.nickName.get() + "!");

        System.out.println();

        System.out.println("Cart[" + this.scope.cartItemCount.get() + "]");
        System.out.println();

        this.printPurchases();
        this.printProducts();

        System.out.println("---------------------------------------------------------");
    }

    public void printPurchases() {
        var purchasesScope = this.getPurchasesPanelScope();
        for (final PurchaseInfo purchase : purchasesScope.purchases.get()) {
            System.out.println("PURCHASE #" + purchase.id);
            System.out.println("Purchase date: " + new Date(purchase.date));
            System.out.println("Items purchased: " + purchase.items);
            System.out.println("Total: R$ " + purchase.total);
            System.out.println();
        }

    }

    public void printProducts() {
        var productsScope = this.getProductsPanelScope();
        for (final ProductInfo product : productsScope.products.get()) {
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
