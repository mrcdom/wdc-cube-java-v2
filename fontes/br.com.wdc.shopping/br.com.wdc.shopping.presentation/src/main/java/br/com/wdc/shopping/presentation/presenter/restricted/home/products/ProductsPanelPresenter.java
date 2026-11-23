package br.com.wdc.shopping.presentation.presenter.restricted.home.products;

import java.util.List;
import java.util.function.Function;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductService;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class ProductsPanelPresenter extends AbstractChildPresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ProductsPanelPresenter.class);

    // :: Public Static Fields

    public static Function<ProductsPanelPresenter, CubeView> createView;

    // -------------------------------

    // :: View State

    public static class ProductsPanelViewState extends ViewState {

        public final Prop<List<ProductInfo>> products = prop();

        public ActionOf<Long> onOpenProduct = ActionOf.noop();

    }

    public final ProductsPanelViewState state = bind(new ProductsPanelViewState());

    // :: Public Instance Fields

    public final HomePresenter owner;

    // :: Internal Instance Fields

    private final ProductService productService;

    // :: Constructor

    public ProductsPanelPresenter(ShoppingApplication app, HomePresenter owner) {
        super(app);
        this.owner = owner;
        this.productService = new ProductService(app);
        this.state.onOpenProduct = actionOf(1, FormParam.asLong("p.productId"), owner::openProduct);
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

    // :: Data load

    public void loadProducts() {
        try {
            this.state.products.set(productService.loadProductsWithoutDescription(1000));
        } catch (Exception caught) {
            LOG.error("Failed to load products", caught);
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("a1b2c3d4e5f6");
    }

}
