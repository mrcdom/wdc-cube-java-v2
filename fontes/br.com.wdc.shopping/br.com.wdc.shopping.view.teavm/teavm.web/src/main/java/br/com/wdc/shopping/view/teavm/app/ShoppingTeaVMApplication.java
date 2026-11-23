package br.com.wdc.shopping.view.teavm.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.AuthenticationService.AuthResult;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootKeys;
import br.com.wdc.shopping.presentation.presenter.RootScope;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginScope;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.shopping.view.teavm.commons.interop.Console;
import br.com.wdc.shopping.view.teavm.infra.BrowserCryptoProvider;
import br.com.wdc.shopping.view.teavm.infra.BrowserLocalStorage;
import br.com.wdc.shopping.view.teavm.infra.BrowserSessionStorage;
import br.com.wdc.shopping.view.teavm.infra.FetchHttpTransport;
import br.com.wdc.shopping.view.teavm.infra.IntentSigner;
import br.com.wdc.shopping.view.teavm.infra.ScheduledExecutorBrowser;
import br.com.wdc.shopping.view.teavm.repo.TeaVMAuthenticationService;
import br.com.wdc.shopping.view.teavm.repo.TeaVMRepositoryBootstrap;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.views.RootView;
import br.com.wdc.shopping.view.teavm.views.cart.CartItemView;
import br.com.wdc.shopping.view.teavm.views.cart.CartView;
import br.com.wdc.shopping.view.teavm.views.home.HomeView;
import br.com.wdc.shopping.view.teavm.views.home.ProductsPanelView;
import br.com.wdc.shopping.view.teavm.views.home.PurchasesPanelView;
import br.com.wdc.shopping.view.teavm.views.login.LoginView;
import br.com.wdc.shopping.view.teavm.views.product.ProductView;
import br.com.wdc.shopping.view.teavm.views.receipt.ReceiptView;

/**
 * Implementação de {@link ShoppingApplication} para o browser via TeaVM. Usa Material Web components para UI e
 * requestAnimationFrame para render loop.
 */
public class ShoppingTeaVMApplication extends ShoppingApplication {

    private static final Log LOG = Log.getLogger(ShoppingTeaVMApplication.class);

    private final String apiBaseUrl;
    private final List<AbstractViewTeaVM<?>> dirtyViews = new ArrayList<>();
    private final IntentSigner intentSigner;
    private boolean renderScheduled;
    private boolean navigatingFromBrowser;

    /** The view of each scope: the presenters', the segments' and the rows'. */
    private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
            .register(RootScope.class, RootView::new)
            .register(LoginScope.class, LoginView::new)
            .register(HomeScope.class, HomeView::new)
            .register(ProductScope.class, ProductView::new)
            .register(CartScope.class, CartView::new)
            .register(ReceiptScope.class, ReceiptView::new)
            .register(CartScope.CartItemScope.class, (ShoppingTeaVMApplication app, CartScope.CartItemScope row) -> new CartItemView(app, row))
            .register(HomeScope.ProductsPanelScope.class, (ShoppingTeaVMApplication app, HomeScope.ProductsPanelScope s) -> new ProductsPanelView(app, s))
            .register(HomeScope.PurchasesPanelScope.class, (ShoppingTeaVMApplication app, HomeScope.PurchasesPanelScope s) -> new PurchasesPanelView(app, s));

    public ShoppingTeaVMApplication(String apiBaseUrl, String shellId) {
        this.setScopeViews(SCOPE_VIEWS);
        this.apiBaseUrl = apiBaseUrl;
        this.persistentStore = new BrowserLocalStorage(shellId);

        // Configura CryptoProvider para browser (antes de qualquer auth)
        CryptoProvider.BEAN.set(new BrowserCryptoProvider());

        // Gera secret aleatório por sessão para assinatura de URL
        this.intentSigner = new IntentSigner(generateRandomBytes(32));

        // Configura ScheduledExecutor para browser
        ScheduledExecutor.BEAN.set(new ScheduledExecutorBrowser());

        // Configura HTTP transport e registra repositórios.
        // Auth tokens (accessToken, refreshToken) são persistidos em localStorage
        // para que a sessão sobreviva ao fechar e reabrir o browser.
        HttpTransport transport = new FetchHttpTransport(apiBaseUrl);
        TeaVMRepositoryBootstrap.initialize(transport, persistentStore);

        // Escuta popstate (Back/Forward do browser)
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
     * Marca uma view como dirty para re-render no próximo frame.
     */
    public void markDirty(AbstractViewTeaVM<?> view) {
        long now = System.currentTimeMillis();
        if (view.dirtyTimestamp < now) {
            view.dirtyTimestamp = now;
            dirtyViews.add(view);
        }
        scheduleRender();
    }

    private void scheduleRender() {
        if (!renderScheduled) {
            renderScheduled = true;
            Window.requestAnimationFrame(t -> this.flushDirtyViews());
        }
    }

    private void flushDirtyViews() {
        renderScheduled = false;
        List<AbstractViewTeaVM<?>> snapshot = new ArrayList<>(dirtyViews);
        dirtyViews.clear();
        for (AbstractViewTeaVM<?> view : snapshot) {
            try {
                view.doUpdate();
            } catch (Exception e) {
                Console.error("Render error in " + view.instanceId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Inicia a aplicação. Tenta restaurar a sessão anterior via refresh token.
     * Se bem-sucedido, navega direto para home; caso contrário, vai para login.
     */
    public void start() {
        Console.log("ShoppingTeaVMApplication starting...");
        new Thread(() -> {
            try {
                var authService = (TeaVMAuthenticationService) AuthenticationService.BEAN.get();
                AuthResult restored = authService.tryRestore();
                if (restored != null && restored.userId() != null) {
                    // Restaurar o subject
                    var users = getUserRepository().fetch(new UserCriteria()
                            .withUserId(restored.userId())
                            .withProjection(Subject.projection()), 0, 1);
                    if (!users.isEmpty()) {
                        setSubject(Subject.create(users.get(0)));
                    }
                }
            } catch (Exception e) {
                LOG.warn("Session restore failed: " + e.getMessage());
            }

            // Navegar para o hash da URL se presente, senão para root
            var hash = getLocationHash();
            if (hash != null && hash.startsWith("#") && hash.length() > 1) {
                // No boot, o secret é novo (F5 gera outro), então strip assinatura antiga se houver
                var location = intentSigner.stripSignature(hash.substring(1));
                try {
                    this.go(location);
                } catch (Exception e) {
                    LOG.warn("Failed to navigate to hash '" + location + "': " + e.getMessage());
                    new RootKeys(this).flip();
                }
            } else {
                // Tauri native: WebView inicia sem hash — restaura último intent salvo.
                // No browser isso só acontece na primeira visita; neste caso savedIntent é null.
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
            this.viewOf(rootPresenter.state);
        }

        // Mark all active views as dirty for UI refresh
        for (var view : this.liveViews()) {
            if (view instanceof AbstractViewTeaVM<?> teavmView) {
                this.markDirty(teavmView);
            }
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
                // Persiste o intent para restauração no próximo launch (Tauri native).
                // No browser, o hash já cobre isso; aqui é redundante mas inofensivo.
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

        // Verifica assinatura - rejeita URLs adulteradas e restaura hash legítimo
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
