package br.com.wdc.shopping.presentation.presenter.restricted.products;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class ProductPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ProductPresenter.class);

    // :: Public Class Fields

    public static Function<ProductPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class ProductViewState implements ViewState {

        public ProductInfo product;
        public int errorCode;
        public String errorMessage;

        public ActionOf<Integer> onAddToCart = ActionOf.noop();
        public Action onOpenProducts = Action.NOOP;

    }

    public final ProductViewState state = new ProductViewState();

    // :: Internal Instance Fields

    private final ProductService productService;
    private CubeViewSlot ownerSlot;

    // Constructor

    public ProductPresenter(ShoppingApplication app) {
        super(app);
        this.productService = new ProductService(app);
        this.state.onAddToCart = actionOf(this::addToCart);
        this.state.onOpenProducts = action(() -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var keys = new ProductKeys(this.app, intent);
        var oldProductId = this.state.product != null ? this.state.product.id : null;

        var newProductId = keys.productId() != null ? keys.productId() : oldProductId;
        if (newProductId == null) {
            // No usable id in the address: nothing to show; go to the home, with the intent in transit
            new HomeKeys(this.app, intent).flip();
            return false;
        }

        if (this.state.product == null || !Objects.equals(newProductId, oldProductId)) {
            var product = productService.loadProductById(newProductId);
            this.state.product = product;
            this.update();
        }

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = keys.parentSlot();
        if (initialization) {

            this.state.errorCode = 0;
            this.state.errorMessage = null;
            if (this.state.product == null) {
                throw new AssertionError("Missing Product");
            }

            this.view = createView.apply(this);
            this.update();
        }

        this.ownerSlot.setView(this.view);

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        new ProductKeys(this.app, intent).withProductId(this.state.product != null ? this.state.product.id : null);
    }

    // :: User Actions

    private void addToCart(Integer quantity) {
        if (quantity == null) {
            this.errorInvalidQuantity();
            LOG.warn("addToCart.errorInvalidQuantity: {}", this.state.errorMessage);
            return;
        }

        if (quantity < 1) {
            this.alertCartItemWidthLessThanOneItem();
            LOG.warn("addToCart.alertCartItemWidthLessThanOneItem: {}", this.state.errorMessage);
            return;
        }

        try {
            app.getCart().addProduct(this.state.product, quantity);

            new CartKeys(this.app).flip();
        } catch (OfflineException caught) {
            this.alertDatabaseNotAvailable();
            LOG.error(this.state.errorMessage, caught);
        }
    }

    // :: Messages

    private void alertDatabaseNotAvailable() {
        this.state.errorCode = 1;
        this.state.errorMessage = "Carrinho não acessível. Aguarde alguns instantes e tente novamente.";
        this.update();
    }

    private void alertCartItemWidthLessThanOneItem() {
        this.state.errorCode = 2;
        this.state.errorMessage = "A quantidade de itens no carrinho deve ser maior ou igual a 1 (um).";
        this.update();
    }

    private void errorInvalidQuantity() {
        this.state.errorCode = 3;
        this.state.errorMessage = "Codificação errada da quantidade.";
        this.update();
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "48b693f67410";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                switch (eventCode) {
                case 1 -> state.onOpenProducts.run();
                case 2 -> state.onAddToCart.run(CoerceUtils.asInteger(formData.get("p.quantity")));
                default -> new AssertionError("eventCode(" + eventCode + ") not handled");
                }
            }
        };
    }

}
