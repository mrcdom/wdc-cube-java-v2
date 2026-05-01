package br.com.wdc.shopping.presentation.presenter.restricted.home;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;

public class HomePresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(HomePresenter.class);

    // :: Public Class Fields

    public static Function<HomePresenter, CubeView> createView;

    // :: Public Instance Fields

    public final HomeViewState state = new HomeViewState();

    // :: Internal Instance Fields

    private final CubeViewSlot contentSlot;

    private CubeViewSlot ownerSlot;
    private CartManager cart;
    private ProductsPanelPresenter productsPanel;
    private PurchasesPanelPresenter purchasesPanel;
    private ThrowingRunnable onCartCommitListenerRemover;
    private ThrowingRunnable onCartChangeListenerRemover;

    // :: Constructor

    public HomePresenter(ShoppingApplication app) {
        super(app);
        this.contentSlot = this::setContentView;
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();
    }

    // :: Cube API

    @Override
    public void release() {
        this.state.contentView = null;

        if (this.productsPanel != null) {
            this.productsPanel.release();
            this.productsPanel = null;
            this.state.productsPanelView = null;
        }

        if (this.purchasesPanel != null) {
            this.purchasesPanel.release();
            this.purchasesPanel = null;
            this.state.purchasesPanelView = null;
        }

        this.app.setCart(null);

        this.onCartCommitListenerRemover.run();
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();

        this.onCartChangeListenerRemover.run();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();

        if (this.view != null) {
            this.view.release();
            this.view = null;
        }
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (this.app.getSubject() == null) {
            Routes.login(this.app, intent);
            return false;
        }

        if (initialization || this.view == null) {
            this.ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);
            this.view = createView.apply(this);

            this.state.nickName = this.app.getSubject().getNickName();

            this.productsPanel = new ProductsPanelPresenter(this.app, this);
            this.state.productsPanelView = this.productsPanel.initialize();

            this.purchasesPanel = new PurchasesPanelPresenter(this.app, this);
            this.state.purchasesPanelView = this.purchasesPanel.initialize();

            this.cart = new CartManager(app);
            this.onCartCommitListenerRemover = this.cart.addCommitListener(this::onCartCommited);
            this.onCartChangeListenerRemover = this.cart.addChangeListener(this::onCartChanged);
            this.app.setCart(this.cart);
            this.update();
        }

        if (this.ownerSlot == null) {
            return false;
        }

        this.ownerSlot.setView(this.view);

        if (deepest) {
            this.setContentView(null);
        } else {
            intent.setViewSlot(PlaceAttributes.SLOT_OWNER, this.contentSlot);
        }

        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        // NOOP
    }

    @Override
    public void commitComputedState() {
        var newCartItemCount = this.cart != null ? this.cart.getItemCount() : 0;
        if (this.state.cartItemCount != newCartItemCount) {
            this.state.cartItemCount = newCartItemCount;
            this.update();
        }
    }

    // :: User Actions

    private void onCartCommited() {
        if (this.productsPanel != null) {
            this.productsPanel.loadProducts();
        }
        if (this.purchasesPanel != null) {
            this.purchasesPanel.onPageChange(0);
        }
    }

    private void onCartChanged() {
        this.state.cartItemCount = this.cart.getItemCount();
        this.update();
    }

    public void onOpenReceipt(Long purchaseId) {
        try {
            if (purchaseId == null) {
                this.alertPurchaseIdRequired();
                LOG.warn("onOpenReceipt: {}", this.state.errorMessage);
                return;
            }

            var intent = this.app.newIntent();
            intent.setParameter(PlaceParameters.PURCHASE_ID, purchaseId);
            Routes.receipt(this.app, intent);
        } catch (Exception caught) {
            if (caught instanceof ProductNotFoundException) {
                this.alertPurchaseNotFound();
                LOG.warn("{}: purchaseId={}", this.state.errorMessage, purchaseId);
                return;
            }

            this.app.alertUnexpectedError(LOG, "Trying to go to receipt place to show purchaseId=" + purchaseId,
                    caught);
        }
    }

    public void onOpenProduct(Long productId) {
        try {
            if (productId == null) {
                this.alertProductIdRequired();
                LOG.warn("onOpenProduct: {}", this.state.errorMessage);
                return;
            }

            var intent = this.app.newIntent();
            intent.setParameter(PlaceParameters.PRODUCT_ID, productId);
            Routes.product(this.app, intent);
        } catch (Exception caught) {
            if (caught instanceof ProductNotFoundException) {
                this.alertProductNotFound();
                LOG.warn("{}: productId={}", this.state.errorMessage, productId);
                return;
            }

            this.app.alertUnexpectedError(LOG, "Trying to go to product place to show productId=" + productId, caught);
        }
    }

    public void onOpenCart() {
        try {
            Routes.cart(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Trying to go to cart place", caught);
        }
    }

    public void onExit() {
        try {
            this.cart.clear();
            this.app.setSubject(null);
            this.setContentView(null);

            Routes.login(this.app);
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, "Trying to go to login place", caught);
        }
    }

    // :: Messages

    private void alertProductNotFound() {
        this.state.errorCode = 3;
        this.state.errorMessage = "Código do produto não localizado.";
        this.update();
    }

    private void alertPurchaseNotFound() {
        this.state.errorCode = 5;
        this.state.errorMessage = "Código do recibo não localizado.";
        this.update();
    }

    private void alertProductIdRequired() {
        this.state.errorCode = 6;
        this.state.errorMessage = "Código do produto é um argumento obrigatório.";
        this.update();
    }

    private void alertPurchaseIdRequired() {
        this.state.errorCode = 7;
        this.state.errorMessage = "Código do recibo é um argumento obrigatório.";
        this.update();
    }

    // :: Slots

    private void setContentView(CubeView view) {
        if (this.state.contentView != view) {
            this.state.contentView = view;
            this.update();
        }
    }

}
