package br.com.wdc.store.presentation.presenter.restricted.products;

import java.util.Objects;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.store.i18n.Messages;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.store.presentation.presenter.restricted.products.calls.ProductQuery;

public class ProductPresenter extends AbstractCubePresenter<StoreApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(ProductPresenter.class);

    // :: Public Instance Fields

    public final ProductScope scope = bind(new ProductScope());

    // :: Internal Instance Fields

    private ScopeSlot ownerSlot;

    // Constructor

    public ProductPresenter(StoreApplication app) {
        super(app);
        this.scope.onOpenProducts = action(1, () -> new HomeKeys(this.app).flip());
        this.scope.onAddToCart = actionOf(2, FormParam.asInteger("p.quantity"), this::addToCart);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var keys = new ProductKeys(this.app, intent);
        var oldProductId = this.scope.product.get() != null ? this.scope.product.get().id : null;

        var newProductId = keys.productId() != null ? keys.productId() : oldProductId;
        if (newProductId == null) {
            // No usable id in the address: nothing to show; go to the home, with the intent in transit
            new HomeKeys(this.app, intent).flip();
            return false;
        }

        if (this.scope.product.get() == null || !Objects.equals(newProductId, oldProductId)) {
            var product = new ProductQuery()
                    .withProductId(newProductId)
                    .run();
            this.scope.product.set(product);
        }

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = keys.parentSlot();
        if (initialization) {

            this.scope.errorCode.set(0);
            this.scope.errorMessage.set(null);
            if (this.scope.product.get() == null) {
                throw new AssertionError("Missing Product");
            }
            this.update();
        }

        this.ownerSlot.setScope(this.scope);

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        new ProductKeys(this.app, intent)
                .withProductId(this.scope.product.get() != null ? this.scope.product.get().id : null);
    }

    // :: User Actions

    private void addToCart(Integer quantity) {
        if (quantity == null) {
            this.errorInvalidQuantity();
            LOG.warn("addToCart.errorInvalidQuantity: {}", this.scope.errorMessage.get());
            return;
        }

        if (quantity < 1) {
            this.alertCartItemWidthLessThanOneItem();
            LOG.warn("addToCart.alertCartItemWidthLessThanOneItem: {}", this.scope.errorMessage.get());
            return;
        }

        try {
            app.getCart().addProduct(this.scope.product.get(), quantity);

            new CartKeys(this.app).flip();
        } catch (OfflineException caught) {
            this.alertDatabaseNotAvailable();
            LOG.error("addToCart: the cart is unavailable", caught);
        }
    }

    // :: Messages

    private void alertDatabaseNotAvailable() {
        this.scope.errorCode.set(1);
        this.scope.errorMessage.set(Messages.errorCartUnavailable());
    }

    private void alertCartItemWidthLessThanOneItem() {
        this.scope.errorCode.set(2);
        this.scope.errorMessage.set(Messages.errorCartQuantityAtLeastOne());
    }

    private void errorInvalidQuantity() {
        this.scope.errorCode.set(3);
        this.scope.errorMessage.set(Messages.errorCartQuantityMalformed());
    }

}
