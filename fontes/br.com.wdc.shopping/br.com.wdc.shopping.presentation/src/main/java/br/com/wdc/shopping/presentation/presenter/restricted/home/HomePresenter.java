package br.com.wdc.shopping.presentation.presenter.restricted.home;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;
import br.com.wdc.shopping.presentation.StorageKeys;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelService;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductService;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class HomePresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(HomePresenter.class);

    // :: Public Class Fields

    public static Function<HomePresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class HomeScope extends Scope {

        /**
         * The products panel: a segment of the home, with no presenter of its own. The home presenter binds its
         * action and attaches it to a view.
         */
        public static class ProductsPanelScope extends Scope {

            public static final String CLASS_ID = "a1b2c3d4e5f6";

            public static BiFunction<ShoppingApplication, ProductsPanelScope, CubeView> createView;

            public final Prop<List<ProductInfo>> products = prop();

            public ActionOf<Long> onOpenProduct = ActionOf.noop();

        }

        /** The purchases panel: a segment of the home, like {@link ProductsPanelScope}. */
        public static class PurchasesPanelScope extends Scope {

            public static final String CLASS_ID = "b3c4d5e6f7a8";

            public static BiFunction<ShoppingApplication, PurchasesPanelScope, CubeView> createView;

            public final Prop<List<PurchaseInfo>> purchases = prop(Collections.emptyList());
            public final Prop<Integer> page = prop(0);
            public final Prop<Integer> pageSize = prop(-1);
            public final Prop<Integer> totalCount = prop(0);

            public ActionOf<Integer> onPageChange = ActionOf.noop();
            public ActionOf<Integer> onItemSizeCapacityChanged = ActionOf.noop();
            public ActionOf<Long> onOpenReceipt = ActionOf.noop();

        }

        public final Prop<CubeView> contentView = prop();
        public final Prop<ProductsPanelScope> productsPanel = prop();
        public final Prop<PurchasesPanelScope> purchasesPanel = prop();
        public final Prop<String> nickName = prop();
        public final Prop<Integer> cartItemCount = prop(0);
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();

        public Action onOpenCart = Action.NOOP;
        public Action onExit = Action.NOOP;

    }

    public final HomeScope state = bind(new HomeScope());

    // :: Internal Instance Fields

    private final CubeViewSlot contentSlot;

    private CubeViewSlot ownerSlot;
    private CartManager cart;
    private final HomeScope.ProductsPanelScope productsPanel = new HomeScope.ProductsPanelScope();
    private final HomeScope.PurchasesPanelScope purchasesPanel = new HomeScope.PurchasesPanelScope();
    private final ProductService productService;
    private final PurchasesPanelService purchasesPanelService;
    private ThrowingRunnable onCartCommitListenerRemover;
    private ThrowingRunnable onCartChangeListenerRemover;

    // :: Constructor

    public HomePresenter(ShoppingApplication app) {
        super(app);
        this.contentSlot = this::setContentView;
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();
        this.productService = new ProductService(app);
        this.purchasesPanelService = new PurchasesPanelService(app);

        this.state.onExit = action(1, this::exit);
        this.state.onOpenCart = action(2, () -> new CartKeys(this.app).flip());

        // The segments' actions: same codes as when each panel had a presenter, now on the segment's own view
        var products = this.productsPanel;
        products.onOpenProduct = actionOf(products, 1, FormParam.asLong("p.productId"), this::openProduct);

        var purchases = this.purchasesPanel;
        purchases.onOpenReceipt = actionOf(purchases, 1, FormParam.asLong("p.purchaseId"), this::openReceipt);
        purchases.onPageChange = actionOf(purchases, 2, FormParam.asInteger("p.page"), this::changePage);
        purchases.onItemSizeCapacityChanged = actionOf(purchases, 3, FormParam.asInteger("p.capacity"),
                this::changeItemSizeCapacity);
    }

    // :: Cube API

    @Override
    public void release() {
        this.state.contentView.set(null);
        this.state.productsPanel.set(null);
        this.state.purchasesPanel.set(null);

        this.app.setCart(null);

        this.onCartCommitListenerRemover.run();
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();

        this.onCartChangeListenerRemover.run();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();

        // Releases the segments' views and this presenter's
        super.release();
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

            this.state.productsPanel.set(attach(this.productsPanel, HomeScope.ProductsPanelScope.createView));
            this.loadProducts();

            // The purchases load once its view measures how many fit (onItemSizeCapacityChanged)
            this.state.purchasesPanel.set(attach(this.purchasesPanel, HomeScope.PurchasesPanelScope.createView));

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
        this.loadProducts();
        this.changePage(0);
    }

    private void onCartChanged() {
        this.state.cartItemCount.set(this.cart.getItemCount());
    }

    private void openReceipt(Long purchaseId) {
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

    private void openProduct(Long productId) {
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
            if (view == null && this.purchasesPanel.view() != null) {
                this.loadPurchases();
            }
        }
    }

    // :: Purchases panel

    private void changePage(int page) {
        this.purchasesPanel.page.set(Math.max(0, page));
        this.loadPurchases();
    }

    /**
     * The view tells how many items fit without scrolling. Only reloads when the capacity actually changed.
     */
    private void changeItemSizeCapacity(int capacity) {
        int newPageSize = Math.max(1, capacity);
        if (newPageSize != this.purchasesPanel.pageSize.get()) {
            this.purchasesPanel.pageSize.set(newPageSize);
            this.purchasesPanel.page.set(0);
            this.loadPurchases();
        }
    }

    private void loadPurchases() {
        try {
            var subject = this.app.getSubject();
            if (subject != null) {
                var panel = this.purchasesPanel;
                var result = purchasesPanelService.fetchPageOfUser(subject.getId(), panel.page.get(),
                        panel.pageSize.get());

                // If requested page exceeds total, clamp to last page and refetch
                if (result.items().isEmpty() && result.totalPages() > 0) {
                    result = purchasesPanelService.fetchPageOfUser(subject.getId(), result.totalPages() - 1,
                            panel.pageSize.get());
                }

                panel.page.set(result.page());
                panel.totalCount.set(result.totalItems());
                panel.purchases.set(result.items());
            }
        } catch (Exception caught) {
            LOG.error("Failed to load purchases", caught);
        }
    }

    // :: Products panel

    private void loadProducts() {
        try {
            this.productsPanel.products.set(productService.loadProductsWithoutDescription(1000));
        } catch (Exception caught) {
            LOG.error("Failed to load products", caught);
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("473dbdd7a36a");
    }

}
