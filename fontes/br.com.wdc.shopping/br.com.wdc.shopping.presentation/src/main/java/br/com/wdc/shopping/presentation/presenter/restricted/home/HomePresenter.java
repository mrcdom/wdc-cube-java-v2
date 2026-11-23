package br.com.wdc.shopping.presentation.presenter.restricted.home;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.StorageKeys;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartManager;
import br.com.wdc.shopping.presentation.presenter.restricted.home.calls.PurchasesPageQuery;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.products.calls.ProductListQuery;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;

public class HomePresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(HomePresenter.class);

    // :: Public Instance Fields

    public final HomeScope state = bind(new HomeScope());

    // :: Internal Instance Fields

    private final ScopeSlot contentSlot;

    private ScopeSlot ownerSlot;
    private CartManager cart;
    private final HomeScope.ProductsPanelScope productsPanel = new HomeScope.ProductsPanelScope();
    private final HomeScope.PurchasesPanelScope purchasesPanel = new HomeScope.PurchasesPanelScope();
    private ThrowingRunnable onCartCommitListenerRemover;
    private ThrowingRunnable onCartChangeListenerRemover;

    // :: Constructor

    public HomePresenter(ShoppingApplication app) {
        super(app);
        this.contentSlot = this::setContent;
        this.onCartCommitListenerRemover = ThrowingRunnable.noop();
        this.onCartChangeListenerRemover = ThrowingRunnable.noop();

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
        this.state.content.set(null);
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

        if (initialization) {
            this.ownerSlot = keys.parentSlot();

            this.state.nickName.set(this.app.getSubject().getNickName());

            this.state.productsPanel.set(this.productsPanel);
            this.loadProducts();

            // The purchases load once its view measures how many fit (onItemSizeCapacityChanged)
            this.state.purchasesPanel.set(this.purchasesPanel);

            this.cart = new CartManager();
            this.onCartCommitListenerRemover = this.cart.addCommitListener(this::onCartCommited);
            this.onCartChangeListenerRemover = this.cart.addChangeListener(this::onCartChanged);
            this.app.setCart(this.cart);
            this.update();
        }

        if (this.ownerSlot == null) {
            return false;
        }

        this.ownerSlot.setScope(this.state);

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
            new ReceiptKeys(this.app)
                    .withPurchaseId(purchaseId)
                    .flip();
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
            new ProductKeys(this.app)
                    .withProductId(productId)
                    .flip();
        } catch (ProductNotFoundException caught) {
            this.alertProductNotFound();
            LOG.warn("{}: productId={}", this.state.errorMessage.get(), productId);
        }
    }

    private void exit() {
        this.cart.clear();
        this.app.setSubject(null);
        this.setContent(null);

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

    private void setContent(Scope content) {
        if (this.state.content.get() != content) {
            this.state.content.set(content);

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
