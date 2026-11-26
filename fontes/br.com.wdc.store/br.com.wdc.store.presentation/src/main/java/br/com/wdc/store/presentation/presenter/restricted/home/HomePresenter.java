package br.com.wdc.store.presentation.presenter.restricted.home;

import java.util.Objects;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.StorageKeys;
import br.com.wdc.store.presentation.exception.ProductNotFoundException;
import br.com.wdc.store.presentation.exception.PurchaseNotFoundException;
import br.com.wdc.store.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.store.presentation.presenter.restricted.home.calls.PurchasesPageQuery;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.store.presentation.presenter.restricted.products.calls.ProductListQuery;
import br.com.wdc.store.presentation.presenter.restricted.products.calls.ProductQuery;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptKeys;

public class HomePresenter extends AbstractCubePresenter<StoreApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(HomePresenter.class);

    // :: Public Instance Fields

    public final HomeScope scope = bind(new HomeScope());

    // :: Internal Instance Fields

    private final ScopeSlot contentSlot;

    private ScopeSlot ownerSlot;
    private CartManager cart;
    private final HomeScope.ProductsPanelScope productsPanel = new HomeScope.ProductsPanelScope();
    private final HomeScope.PurchasesPanelScope purchasesPanel = new HomeScope.PurchasesPanelScope();
    private ThrowingRunnable onCartCommitListenerRemover;
    private ThrowingRunnable onCartChangeListenerRemover;

    // :: Constructor

    public HomePresenter(StoreApplication app) {
        super(app);
        this.contentSlot = this::setContent;
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();

        this.scope.onExit = action(1, this::exit);
        this.scope.onOpenCart = action(2, () -> new CartKeys(this.app).flip());

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
        this.app.setCart(null);

        this.onCartCommitListenerRemover.run();
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();

        this.onCartChangeListenerRemover.run();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        var keys = new HomeKeys(this.app, intent);
        if (this.app.getSubject() == null) {
            new LoginKeys(this.app, intent).flip();
            return false;
        }

        if (initialization) {
            this.ownerSlot = keys.parentSlot();

            this.scope.nickName.set(this.app.getSubject().getNickName());

            this.scope.productsPanel.set(this.productsPanel);
            this.loadProducts();

            // The purchases load once its view measures how many fit (onItemSizeCapacityChanged)
            this.scope.purchasesPanel.set(this.purchasesPanel);

            this.cart = new CartManager();
            this.restoreCart();
            this.onCartCommitListenerRemover = this.cart.addCommitListener(this::onCartCommited);
            this.onCartChangeListenerRemover = this.cart.addChangeListener(this::onCartChanged);
            this.app.setCart(this.cart);
            this.scope.cartItemCount.set(this.cart.getItemCount());
            this.update();
        }

        if (this.ownerSlot == null) {
            return false;
        }

        this.ownerSlot.setScope(this.scope);

        if (deepest) {
            this.setContent(null);
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
        this.scope.cartItemCount.set(newCartItemCount);
    }

    // :: User Actions

    private void onCartCommited() {
        this.loadProducts();
        this.changePage(0);
    }

    private void onCartChanged() {
        this.scope.cartItemCount.set(this.cart.getItemCount());
        this.saveCart();
    }

    /** Restores the cart the session kept: a page reload starts the application again, not the session. */
    private void restoreCart() {
        var snapshot = CartManager.Snapshot.parse(this.app.clientSessionStore().get(StorageKeys.CART));
        if (snapshot == null || !Objects.equals(snapshot.userId(), this.app.getSubject().getId())) {
            return;
        }
        for (var entry : snapshot.quantities().entrySet()) {
            try {
                var product = new ProductQuery()
                        .withProductId(entry.getKey())
                        .run();
                this.cart.addProduct(product, entry.getValue());
            } catch (ProductNotFoundException caught) {
                LOG.info("restoreCart: product {} is no longer available", entry.getKey());
            }
        }
    }

    private void saveCart() {
        var store = this.app.clientSessionStore();
        if (this.cart.getSize() == 0) {
            store.remove(StorageKeys.CART);
        } else {
            var snapshot = this.cart.snapshot(this.app.getSubject().getId());
            store.set(StorageKeys.CART, snapshot.format());
        }
    }

    private void openReceipt(Long purchaseId) {
        if (purchaseId == null || purchaseId < 0) {
            this.alertPurchaseIdRequired();
            LOG.warn("openReceipt: {}", this.scope.errorMessage.get());
            return;
        }

        try {
            new ReceiptKeys(this.app)
                    .withPurchaseId(purchaseId)
                    .flip();
        } catch (PurchaseNotFoundException caught) {
            this.alertPurchaseNotFound();
            LOG.warn("{}: purchaseId={}", this.scope.errorMessage.get(), purchaseId);
        }
    }

    private void openProduct(Long productId) {
        if (productId == null || productId < 0) {
            this.alertProductIdRequired();
            LOG.warn("openProduct: {}", this.scope.errorMessage.get());
            return;
        }

        try {
            new ProductKeys(this.app)
                    .withProductId(productId)
                    .flip();
        } catch (ProductNotFoundException caught) {
            this.alertProductNotFound();
            LOG.warn("{}: productId={}", this.scope.errorMessage.get(), productId);
        }
    }

    private void exit() {
        this.cart.clear();
        this.app.clientSessionStore().remove(StorageKeys.CART);
        this.app.setSubject(null);
        this.setContent(null);

        // Remove the persistent token from secure storage
        this.app.clientPersistentStore().secure().remove(StorageKeys.AUTH_TOKEN);

        // Logout through AuthenticationService (clears the tokens from ClientStorage)
        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            var refreshToken = this.app.clientPersistentStore().secure().get(StorageKeys.AUTH_REFRESH_TOKEN);
            authService.logout(refreshToken);
        }

        new LoginKeys(this.app).flip();
    }

    // :: Messages

    private void alertProductNotFound() {
        this.scope.errorCode.set(3);
        this.scope.errorMessage.set("Product code not found.");
    }

    private void alertPurchaseNotFound() {
        this.scope.errorCode.set(5);
        this.scope.errorMessage.set("Receipt code not found.");
    }

    private void alertProductIdRequired() {
        this.scope.errorCode.set(6);
        this.scope.errorMessage.set("Product code is a required argument.");
    }

    private void alertPurchaseIdRequired() {
        this.scope.errorCode.set(7);
        this.scope.errorMessage.set("Receipt code is a required argument.");
    }

    // :: Slots

    private void setContent(Scope content) {
        if (this.scope.content.get() != content) {
            this.scope.content.set(content);

            // Back to the home's own panels: reload the purchases, once the panel measured how many fit
            if (content == null && this.purchasesPanel.pageSize.get() > 0) {
                this.loadPurchases();
            }
        }
    }

    // :: Purchases panel

    private void changePage(Integer page) {
        if (page == null) {
            return;
        }
        this.purchasesPanel.page.set(Math.max(0, page));
        this.loadPurchases();
    }

    /**
     * The view tells how many items fit without scrolling. Only reloads when the capacity actually changed.
     */
    private void changeItemSizeCapacity(Integer capacity) {
        if (capacity == null) {
            return;
        }
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
                var call = new PurchasesPageQuery()
                        .withUserId(subject.getId())
                        .withPage(panel.page.get())
                        .withPageSize(panel.pageSize.get());
                var result = call.run();

                // If requested page exceeds total, clamp to last page and refetch
                if (result.items().isEmpty() && result.totalPages() > 0) {
                    result = call.withPage(result.totalPages() - 1).run();
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
            this.productsPanel.products.set(new ProductListQuery()
                    .withLimit(1000)
                    .run());
        } catch (Exception caught) {
            LOG.error("Failed to load products", caught);
        }
    }

}
