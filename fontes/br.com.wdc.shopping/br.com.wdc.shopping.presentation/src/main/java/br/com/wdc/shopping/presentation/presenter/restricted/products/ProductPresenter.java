package br.com.wdc.shopping.presentation.presenter.restricted.products;

import java.util.Objects;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.business.shared.exception.OfflineException;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class ProductPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(ProductPresenter.class.getName());

    // :: Public Class Fields

    public static Function<ProductPresenter, CubeView> createView;

    // :: Public Instance Fields

    public final ProductViewState state = new ProductViewState();

    // :: Internal Instance Fields

    private CubeViewSlot ownerSlot;

    // Constructor

    public ProductPresenter(ShoppingApplication app) {
        super(app);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var oldProductId = this.state.product != null ? this.state.product.id : null;

        var newProductId = intent.getParameterAsLong(PlaceParameters.PRODUCT_ID, oldProductId);
        if (newProductId == null) {
            throw new AssertionError("Missing PRODUCT_ID");
        }

        if (this.state.product == null || !Objects.equals(newProductId, oldProductId)) {
            var product = ProductService.BEAN.loadProductById(newProductId);
            this.state.product = product;
            this.update();
        }

        if (initialization) {
            this.ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);

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
        if (this.state.product != null) {
            intent.setParameter(PlaceParameters.PRODUCT_ID, this.state.product.id);
        }
    }

    // :: User Actions

    public void onAddToCart(Integer quantity) {
        try {
            if (quantity == null) {
                this.errorInvalidQuantity();
                LOG.warn("onAddToCart.errorInvalidQuantity: {}", this.state.errorMessage);
                return;
            }

            if (quantity < 1) {
                this.alertCartItemWidthLessThanOneItem();
                LOG.warn("onAddToCart.alertCartItemWidthLessThanOneItem: {}", this.state.errorMessage);
                return;
            }

            app.getCart().addProduct(this.state.product, quantity);

            CubeIntent intent = this.app.newIntent();
            Routes.cart(this.app, intent);
        } catch (Exception caught) {
            if (caught instanceof OfflineException) {
                this.alertDatabaseNotAvailable();
                LOG.error(this.state.errorMessage, caught);
                return;
            }

            app.alertUnexpectedError(LOG, "Adding an item to cart", caught);
        }
    }

    public void onOpenProducts() {
        try {
            Routes.home(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Going to home of restricted place", caught);
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

}
