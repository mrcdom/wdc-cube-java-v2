package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings("java:S106")
public class ProductViewMock extends AbstractViewMock<ProductPresenter> {

    public static ProductViewMock cast(CubeView view) {
        var cls = ProductViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductViewMock) view;
    }

    public ProductViewState state;

    public ProductViewMock(ProductPresenter presenter) {
        super(((ShoppingApplicationMock) presenter.app), presenter);
        this.state = presenter.state;
    }

    public void printProduto() {
        System.out.println("PRODUTO #" + this.state.product.get().id);
        System.out.println("Nome: " + this.state.product.get().name);
        System.out.println("Preço: " + this.state.product.get().price);
        System.out.println("Descrição: " + this.state.product.get().description);
        System.out.println("Imagem: " + this.state.product.get().image);
        System.out.println();
    }

}
