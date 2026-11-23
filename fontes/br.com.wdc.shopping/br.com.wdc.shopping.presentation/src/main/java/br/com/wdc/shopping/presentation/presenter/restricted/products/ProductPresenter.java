package br.com.wdc.shopping.presentation.presenter.restricted.products;

import java.util.Objects;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;

public class ProductPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ProductPresenter.class);

    // :: Public Instance Fields

    public final ProductScope state = bind(new ProductScope());

    // :: Internal Instance Fields

    private final ProductService productService;
    private ScopeSlot ownerSlot;

    // Constructor

    public ProductPresenter(ShoppingApplication app) {
        super(app);
        this.productService = new ProductService(ProductRepository.BEAN.get());
        this.state.onOpenProducts = action(1, () -> new HomeKeys(this.app).flip());
        this.state.onAddToCart = actionOf(2, FormParam.asInteger("p.quantity"), this::addToCart);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var keys = new ProductKeys(this.app, intent);
        var oldProductId = this.state.product.get() != null ? this.state.product.get().id : null;

        var newProductId = keys.productId() != null ? keys.productId() : oldProductId;
        if (newProductId == null) {
            // No usable id in the address: nothing to show; go to the home, with the intent in transit
            new HomeKeys(this.app, intent).flip();
            return false;
        }

        if (this.state.product.get() == null || !Objects.equals(newProductId, oldProductId)) {
            var product = productService.loadProductById(newProductId);
            this.state.product.set(product);
        }

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = keys.parentSlot();
        if (initialization) {

            this.state.errorCode.set(0);
            this.state.errorMessage.set(null);
            if (this.state.product.get() == null) {
                throw new AssertionError("Missing Product");
            }
            this.update();
        }

        this.ownerSlot.setScope(this.state);

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        new ProductKeys(this.app, intent).withProductId(this.state.product.get() != null ? this.state.product.get().id : null);
    }

    // :: User Actions

    private void addToCart(Integer quantity) {
        if (quantity == null) {
            this.errorInvalidQuantity();
            LOG.warn("addToCart.errorInvalidQuantity: {}", this.state.errorMessage.get());
            return;
        }

        if (quantity < 1) {
            this.alertCartItemWidthLessThanOneItem();
            LOG.warn("addToCart.alertCartItemWidthLessThanOneItem: {}", this.state.errorMessage.get());
            return;
        }

        try {
            app.getCart().addProduct(this.state.product.get(), quantity);

            new CartKeys(this.app).flip();
        } catch (OfflineException caught) {
            this.alertDatabaseNotAvailable();
            LOG.error(this.state.errorMessage.get(), caught);
        }
    }

    // :: Messages

    private void alertDatabaseNotAvailable() {
        this.state.errorCode.set(1);
        this.state.errorMessage.set("Carrinho não acessível. Aguarde alguns instantes e tente novamente.");
    }

    private void alertCartItemWidthLessThanOneItem() {
        this.state.errorCode.set(2);
        this.state.errorMessage.set("A quantidade de itens no carrinho deve ser maior ou igual a 1 (um).");
    }

    private void errorInvalidQuantity() {
        this.state.errorCode.set(3);
        this.state.errorMessage.set("Codificação errada da quantidade.");
    }

}
