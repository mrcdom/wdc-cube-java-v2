package br.com.wdc.shopping.presentation.presenter.restricted.home;

import java.util.function.Function;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;
import br.com.wdc.shopping.presentation.StorageKeys;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;

public class HomePresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(HomePresenter.class);

    // :: Public Class Fields

    public static Function<HomePresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class HomeViewState extends ViewState {

        public final Prop<CubeView> contentView = prop();
        public final Prop<CubeView> productsPanelView = prop();
        public final Prop<CubeView> purchasesPanelView = prop();
        public final Prop<String> nickName = prop();
        public final Prop<Integer> cartItemCount = prop(0);
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();

        public Action onOpenCart = Action.NOOP;
        public Action onExit = Action.NOOP;

    }

    public final HomeViewState state = bind(new HomeViewState());

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
        this.state.onExit = action(1, this::exit);
        this.state.onOpenCart = action(2, () -> new CartKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public void release() {
        this.state.contentView.set(null);

        if (this.productsPanel != null) {
            this.productsPanel.release();
            this.productsPanel = null;
            this.state.productsPanelView.set(null);
        }

        if (this.purchasesPanel != null) {
            this.purchasesPanel.release();
            this.purchasesPanel = null;
            this.state.purchasesPanelView.set(null);
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
        var keys = new HomeKeys(this.app, intent);
        if (this.app.getSubject() == null) {
            new LoginKeys(this.app, intent).flip();
            return false;
        }

        if (initialization || this.view == null) {
            this.ownerSlot = keys.parentSlot();
            this.view = createView.apply(this);

            this.state.nickName.set(this.app.getSubject().getNickName());

            this.productsPanel = new ProductsPanelPresenter(this.app, this);
            this.state.productsPanelView.set(this.productsPanel.initialize());

            this.purchasesPanel = new PurchasesPanelPresenter(this.app, this);
            this.state.purchasesPanelView.set(this.purchasesPanel.initialize());

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
            keys.withParentSlot(this.contentSlot);
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
        this.state.cartItemCount.set(newCartItemCount);
    }

    // :: User Actions

    private void onCartCommited() {
        if (this.productsPanel != null) {
            this.productsPanel.loadProducts();
        }
        if (this.purchasesPanel != null) {
            this.purchasesPanel.changePage(0);
        }
    }

    private void onCartChanged() {
        this.state.cartItemCount.set(this.cart.getItemCount());
    }

    /** Called from inside the purchases panel's guarded action. */
    public void openReceipt(Long purchaseId) {
        if (purchaseId == null || purchaseId < 0) {
            this.alertPurchaseIdRequired();
            LOG.warn("openReceipt: {}", this.state.errorMessage.get());
            return;
        }

        try {
            new ReceiptKeys(this.app).withPurchaseId(purchaseId).flip();
        } catch (ProductNotFoundException caught) {
            this.alertPurchaseNotFound();
            LOG.warn("{}: purchaseId={}", this.state.errorMessage.get(), purchaseId);
        }
    }

    /** Called from inside the products panel's guarded action. */
    public void openProduct(Long productId) {
        if (productId == null || productId < 0) {
            this.alertProductIdRequired();
            LOG.warn("openProduct: {}", this.state.errorMessage.get());
            return;
        }

        try {
            new ProductKeys(this.app).withProductId(productId).flip();
        } catch (ProductNotFoundException caught) {
            this.alertProductNotFound();
            LOG.warn("{}: productId={}", this.state.errorMessage.get(), productId);
        }
    }

    private void exit() {
        this.cart.clear();
        this.app.setSubject(null);
        this.setContentView(null);

        // Remover token persistente do storage seguro
        this.app.clientPersistentStore().secure().remove(StorageKeys.AUTH_TOKEN);

        // Logout via AuthenticationService (limpa tokens do ClientStorage)
        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            var refreshToken = this.app.clientPersistentStore().secure().get(StorageKeys.AUTH_REFRESH_TOKEN);
            authService.logout(refreshToken);
        }

        new LoginKeys(this.app).flip();
    }

    // :: Messages

    private void alertProductNotFound() {
        this.state.errorCode.set(3);
        this.state.errorMessage.set("Código do produto não localizado.");
    }

    private void alertPurchaseNotFound() {
        this.state.errorCode.set(5);
        this.state.errorMessage.set("Código do recibo não localizado.");
    }

    private void alertProductIdRequired() {
        this.state.errorCode.set(6);
        this.state.errorMessage.set("Código do produto é um argumento obrigatório.");
    }

    private void alertPurchaseIdRequired() {
        this.state.errorCode.set(7);
        this.state.errorMessage.set("Código do recibo é um argumento obrigatório.");
    }

    // :: Slots

    private void setContentView(CubeView view) {
        if (this.state.contentView.get() != view) {
            this.state.contentView.set(view);

            // Reload purchases when returning to the main view
            if (view == null && this.purchasesPanel != null) {
                this.purchasesPanel.loadPurchases();
            }
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("473dbdd7a36a");
    }

}
