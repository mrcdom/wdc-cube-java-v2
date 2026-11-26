package br.com.wdc.store.view.teavm.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.AuthenticationService.AuthResult;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootKeys;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.view.teavm.commons.interop.Console;
import br.com.wdc.store.view.teavm.infra.BrowserCryptoProvider;
import br.com.wdc.store.view.teavm.infra.BrowserLocalStorage;
import br.com.wdc.store.view.teavm.infra.BrowserSessionStorage;
import br.com.wdc.store.view.teavm.infra.FetchHttpTransport;
import br.com.wdc.store.view.teavm.infra.IntentSigner;
import br.com.wdc.store.view.teavm.infra.ScheduledExecutorBrowser;
import br.com.wdc.store.view.teavm.repo.TeaVMAuthenticationService;
import br.com.wdc.store.view.teavm.repo.TeaVMRepositoryBootstrap;
import br.com.wdc.store.view.teavm.views.AbstractViewTeaVM;
import br.com.wdc.store.view.teavm.views.RootView;
import br.com.wdc.store.view.teavm.views.cart.CartItemView;
import br.com.wdc.store.view.teavm.views.cart.CartView;
import br.com.wdc.store.view.teavm.views.home.HomeView;
import br.com.wdc.store.view.teavm.views.home.ProductsPanelView;
import br.com.wdc.store.view.teavm.views.home.PurchasesPanelView;
import br.com.wdc.store.view.teavm.views.login.LoginView;
import br.com.wdc.store.view.teavm.views.product.ProductView;
import br.com.wdc.store.view.teavm.views.receipt.ReceiptView;

/**
 * {@link StoreApplication} implementation for the browser through TeaVM. Uses Material Web components for the UI and
 * requestAnimationFrame for the render loop.
 */
public class StoreTeaVMApplication extends StoreApplication {

    private static final Log LOG = Log.getLogger(StoreTeaVMApplication.class);

    private final String apiBaseUrl;
    private final List<AbstractViewTeaVM<?>> dirtyViews = new ArrayList<>();
    private final IntentSigner intentSigner;
    private boolean renderScheduled;
    private boolean navigatingFromBrowser;

    /** The view of each scope class: the presenters', the segments' and the rows'. */
    private static final Map<Class<?>, BiFunction<StoreTeaVMApplication, Scope, AbstractViewTeaVM<?>>> VIEWS = new HashMap<>();

    static {
        VIEWS.put(RootScope.class, (app, s) -> new RootView(app, (RootScope) s));
        VIEWS.put(LoginScope.class, (app, s) -> new LoginView(app, (LoginScope) s));
        VIEWS.put(HomeScope.class, (app, s) -> new HomeView(app, (HomeScope) s));
        VIEWS.put(ProductScope.class, (app, s) -> new ProductView(app, (ProductScope) s));
        VIEWS.put(CartScope.class, (app, s) -> new CartView(app, (CartScope) s));
        VIEWS.put(ReceiptScope.class, (app, s) -> new ReceiptView(app, (ReceiptScope) s));
        VIEWS.put(CartScope.CartItemScope.class, (app, s) -> new CartItemView(app, (CartScope.CartItemScope) s));
        VIEWS.put(HomeScope.ProductsPanelScope.class, (app, s) -> new ProductsPanelView(app, (HomeScope.ProductsPanelScope) s));
        VIEWS.put(HomeScope.PurchasesPanelScope.class, (app, s) -> new PurchasesPanelView(app, (HomeScope.PurchasesPanelScope) s));
    }

    /** The view of each scope drawn, until it is released. */
    private final Map<Scope, AbstractViewTeaVM<?>> views = new IdentityHashMap<>();

    public StoreTeaVMApplication(String apiBaseUrl, String shellId) {
        this.apiBaseUrl = apiBaseUrl;
        this.persistentStore = new BrowserLocalStorage(shellId);

        // Sets up the browser CryptoProvider (before any auth)
        CryptoProvider.BEAN.set(new BrowserCryptoProvider());

        // Generates a random per-session secret for URL signing
        this.intentSigner = new IntentSigner(generateRandomBytes(32));

        // Sets up the browser ScheduledExecutor
        ScheduledExecutor.BEAN.set(new ScheduledExecutorBrowser());

        // Sets up the HTTP transport and registers the repositories.
        // Auth tokens (accessToken, refreshToken) are persisted in localStorage
        // so the session survives closing and reopening the browser.
        HttpTransport transport = new FetchHttpTransport(apiBaseUrl);
        TeaVMRepositoryBootstrap.initialize(transport, persistentStore);

        // Listens to popstate (browser Back/Forward)
        Window.current().addEventListener("popstate", evt -> onPopState());
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Resolve an image path to a full URL. In browser mode (same origin), returns the relative path. In Tauri mode
     * (cross-origin), prepends the API base URL.
     */
    public String resolveImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }
        // If apiBaseUrl is empty or matches current origin, use relative path
        if (apiBaseUrl == null || apiBaseUrl.isEmpty()) {
            return "/" + imagePath;
        }
        return apiBaseUrl + "/" + imagePath;
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new HashMap<>();
    }

    @Override
    public String b64Cipher(String text) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String b64Decipher(String b64Text) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String getClientIp() {
        // TeaVM runs entirely in the browser — the client IP is not available
        // on the client side. Server-side logging should use the WebSocket/HTTP
        // connection IP instead.
        return "127.0.0.1";
    }

    private final BrowserSessionStorage sessionStore = new BrowserSessionStorage();
    private final BrowserLocalStorage persistentStore;

    @Override
    public ClientStorage clientSessionStore() {
        return sessionStore;
    }

    @Override
    public ClientStorage clientPersistentStore() {
        return persistentStore;
    }

    /**
     * Marks a view as dirty to re-render on the next frame.
     */
    public void markDirty(AbstractViewTeaVM<?> view) {
        if (view.isReleased()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (view.dirtyTimestamp < now) {
            view.dirtyTimestamp = now;
            dirtyViews.add(view);
        }
        scheduleRender();
    }

    // :: Views

    /**
     * The view that draws the scope, made the first time it is asked for: from then on, a change in the scope redraws
     * that view. {@code null} for no scope.
     */
    public AbstractViewTeaVM<?> viewOf(Scope scope) {
        if (scope == null) {
            return null;
        }
        var existing = this.views.get(scope);
        if (existing != null) {
            return existing;
        }
        var factory = VIEWS.get(scope.getClass());
        if (factory == null) {
            throw new IllegalStateException("No view registered for scope " + scope.getClass().getName());
        }
        var made = factory.apply(this, scope);
        this.views.put(scope, made);
        scope.setUpdater(made.updater);
        // The first drawing
        made.update();
        return made;
    }

    /** The view was released: it no longer draws its scope. */
    public void forgetView(AbstractViewTeaVM<?> view) {
        this.views.remove(view.scope(), view);
        this.dirtyViews.remove(view);
    }

    private void scheduleRender() {
        if (!renderScheduled) {
            renderScheduled = true;
            Window.requestAnimationFrame(t -> this.flushDirtyViews());
        }
    }

    /** One paint cycle: the presenters commit their computed state, then the dirty views redraw. */
    private void flushDirtyViews() {
        renderScheduled = false;
        this.commitComputedState();
        List<AbstractViewTeaVM<?>> snapshot = new ArrayList<>(dirtyViews);
        dirtyViews.clear();
        for (AbstractViewTeaVM<?> view : snapshot) {
            try {
                view.redraw();
            } catch (Exception e) {
                Console.error("Render error in " + view.instanceId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Starts the application. Tries to restore the previous session through the refresh token.
     * If it succeeds, navigates straight to home; otherwise, goes to login.
     */
    public void start() {
        Console.log("StoreTeaVMApplication starting...");
        new Thread(() -> {
            try {
                var authService = (TeaVMAuthenticationService) AuthenticationService.BEAN.get();
                AuthResult restored = authService.tryRestore();
                if (restored != null && restored.userId() != null) {
                    // Restore the subject
                    var users = UserRepository.BEAN.get().fetch(new UserCriteria()
                            .withUserId(restored.userId())
                            .withProjection(Subject.projection()), 0, 1);
                    if (!users.isEmpty()) {
                        setSubject(Subject.create(users.get(0)));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Session restore failed: " + e.getMessage());
            }

            // Navigate to the URL hash if present, otherwise to root
            var hash = getLocationHash();
            if (hash != null && hash.startsWith("#") && hash.length() > 1) {
                // On boot the secret is new (F5 generates another one), so strip any old signature
                var location = intentSigner.stripSignature(hash.substring(1));
                try {
                    this.go(location);
                } catch (Exception e) {
                    LOG.warn("Failed to navigate to hash '" + location + "': " + e.getMessage());
                    new RootKeys(this).flip();
                }
            } else {
                // Tauri native: the WebView starts without a hash — restores the last saved intent.
                // In the browser this only happens on the first visit; in that case savedIntent is null.
                var savedIntent = persistentStore.get("session.intent");
                if (savedIntent != null && !savedIntent.isBlank() && !savedIntent.startsWith("login")) {
                    try {
                        this.go(savedIntent);
                    } catch (Exception e) {
                        LOG.warn("Failed to restore intent '" + savedIntent + "': " + e.getMessage());
                        new RootKeys(this).flip();
                    }
                } else {
                    new RootKeys(this).flip();
                }
            }
        }).start();
    }

    @Override
    public void updateHistory() {
        // The host draws the root presenter's scope; each view asks for the views of the scopes it holds
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            this.viewOf(rootPresenter.scope);
        }

        // Mark all active views as dirty for UI refresh
        for (var view : new ArrayList<>(this.views.values())) {
            this.markDirty(view);
        }

        // Sync browser URL hash with signed intent
        try {
            var intent = this.newIntent();
            var intentStr = intent.toString();
            if (intentStr.equals(this.fragment)) {
                // Every action republishes the address; only a change becomes a history entry
                return;
            }
            this.fragment = intentStr;
            if (!navigatingFromBrowser) {
                var signedUrl = intentSigner.sign(intentStr);
                pushState("#" + signedUrl);
                // Persists the intent for restoration on the next launch (Tauri native).
                // In the browser the hash already covers this; here it is redundant but harmless.
                if (!intentStr.isBlank() && !intentStr.startsWith("login")) {
                    persistentStore.set("session.intent", intentStr);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to push browser history state: " + e.getMessage());
        }
    }

    private void onPopState() {
        var hash = getLocationHash();
        if (hash == null || hash.isEmpty()) {
            return;
        }
        if (hash.startsWith("#")) {
            hash = hash.substring(1);
        }
        if (hash.isEmpty()) {
            return;
        }

        // Checks the signature - rejects tampered URLs and restores the legitimate hash
        if (!intentSigner.verify(hash)) {
            LOG.warn("Invalid URL signature, restoring current state: " + hash);
            pushState("#" + intentSigner.sign(this.fragment));
            return;
        }

        var location = intentSigner.stripSignature(hash);
        new Thread(() -> {
            this.navigatingFromBrowser = true;
            try {
                this.go(location);
            } catch (Exception e) {
                LOG.error("Failed to handle browser navigation to: " + location + " - " + e.getMessage());
            } finally {
                this.navigatingFromBrowser = false;
            }
        }).start();
    }

    @JSBody(params = "url", script = "history.pushState(null, '', url);")
    private static native void pushState(String url);

    @JSBody(params = {}, script = "return window.location.hash || '';")
    private static native String getLocationHash();

    @JSBody(params = "length", script = "var arr = new Uint8Array(length); crypto.getRandomValues(arr); return Array.from(arr);")
    private static native int[] cryptoRandomValues(int length);

    private static byte[] generateRandomBytes(int length) {
        int[] values = cryptoRandomValues(length);
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

}
