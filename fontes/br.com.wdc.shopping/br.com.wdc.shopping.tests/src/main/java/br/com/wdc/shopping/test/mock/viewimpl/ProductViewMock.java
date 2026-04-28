package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
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

    public ProductViewMock(ShoppingApplicationMock app, ProductPresenter presenter) {
        super(app, presenter);
        this.state = presenter.state;
    }

    public void printProduto() {
        System.out.println("PRODUTO #" + this.state.product.id);
        System.out.println("Nome: " + this.state.product.name);
        System.out.println("Preço: " + this.state.product.price);
        System.out.println("Descrição: " + this.state.product.description);
        System.out.println("Imagem: " + this.state.product.image);
        System.out.println();
    }

}
