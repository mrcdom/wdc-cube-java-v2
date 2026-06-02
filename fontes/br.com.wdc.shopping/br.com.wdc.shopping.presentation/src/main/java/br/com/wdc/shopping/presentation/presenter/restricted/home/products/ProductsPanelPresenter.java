package br.com.wdc.shopping.presentation.presenter.restricted.home.products;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractChildPresenter;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
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

    public static class ProductsPanelViewState implements ViewState {

        public List<ProductInfo> products;

    }

    public final ProductsPanelViewState state = new ProductsPanelViewState();

    // :: Public Instance Fields

    public final HomePresenter owner;

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

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "a1b2c3d4e5f6";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                switch (eventCode) {
                case 1 -> onOpenProduct(CoerceUtils.asLong(formData.get("p.productId")));
                default -> new AssertionError("eventCode(" + eventCode + ") not handled");
                }
            }

        };
    }

}
