package br.com.wdc.shopping.presentation.presenter.restricted.home.products;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductService;

public class ProductsPanelPresenter extends AbstractChildPresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(ProductsPanelPresenter.class);

    // :: Public Static Fields

    public static Function<ProductsPanelPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final HomePresenter owner;
    public final ProductsPanelViewState state = new ProductsPanelViewState();

    // :: Internal Instance Fields

    private final ProductService productService;

    // :: Constructor

    public ProductsPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
        this.productService = new ProductService(app);
    }

    // :: Life cycle

    @Override
    protected CubeView onCreateView() {
        return createView.apply(this);
    }

    @Override
    protected void onInitialize() {
        this.loadProducts();
    }

    // :: User Actions

    public void onOpenProduct(Long productId) {
        this.owner.onOpenProduct(productId);
    }

    // :: Data load

    public void loadProducts() {
        try {
            this.state.products = productService.loadProductsWithoutDescription(1000);
            this.update();
        } catch (Exception caught) {
            LOG.error("Failed to load products", caught);
        }
    }

}
