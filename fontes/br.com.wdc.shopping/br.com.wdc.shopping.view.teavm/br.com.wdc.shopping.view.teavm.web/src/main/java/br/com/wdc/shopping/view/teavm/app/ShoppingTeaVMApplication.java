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
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.security.AuthResult;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.CryptoProvider;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.teavm.interop.Console;
import br.com.wdc.shopping.view.teavm.infra.BrowserCryptoProvider;
import br.com.wdc.shopping.view.teavm.infra.BrowserSessionStorage;
import br.com.wdc.shopping.view.teavm.infra.FetchHttpTransport;
import br.com.wdc.shopping.view.teavm.infra.IntentSigner;
import br.com.wdc.shopping.view.teavm.infra.ScheduledExecutorBrowser;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.repo.TeaVMAuthenticationService;
import br.com.wdc.shopping.view.teavm.repo.TeaVMRepositoryBootstrap;
import br.com.wdc.shopping.view.teavm.views.CartView;
import br.com.wdc.shopping.view.teavm.views.HomeView;
import br.com.wdc.shopping.view.teavm.views.LoginView;
import br.com.wdc.shopping.view.teavm.views.ProductView;
import br.com.wdc.shopping.view.teavm.views.ProductsPanelView;
import br.com.wdc.shopping.view.teavm.views.PurchasesPanelView;
import br.com.wdc.shopping.view.teavm.views.ReceiptView;
import br.com.wdc.shopping.view.teavm.views.RootView;

/**
 * Implementação de {@link ShoppingApplication} para o browser via TeaVM. Usa Material Web components para UI e
 * requestAnimationFrame para render loop.
 */
public class ShoppingTeaVMApplication extends ShoppingApplication {

    private static final Log LOG = Log.getLogger(ShoppingTeaVMApplication.class);

    private final String apiBaseUrl;
    private final List<AbstractViewTeaVM<?>> dirtyViews = new ArrayList<>();
    private final Map<String, Object> attributeMap = new HashMap<>();
    private final IntentSigner intentSigner;
    private boolean renderScheduled;
    private boolean navigatingFromBrowser;

    static {
        // Wiring das view factories
        RootPresenter.createView = RootView::new;
        LoginPresenter.createView = LoginView::new;
        HomePresenter.createView = HomeView::new;
        CartPresenter.createView = CartView::new;
        ProductPresenter.createView = ProductView::new;
        ReceiptPresenter.createView = ReceiptView::new;
        ProductsPanelPresenter.createView = ProductsPanelView::new;
        PurchasesPanelPresenter.createView = PurchasesPanelView::new;
    }

    public ShoppingTeaVMApplication(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;

        // Configura CryptoProvider para browser (antes de qualquer auth)
        CryptoProvider.BEAN.set(new BrowserCryptoProvider());

        // Gera secret aleatório por sessão para assinatura de URL
        this.intentSigner = new IntentSigner(generateRandomBytes(32));

        // Configura ScheduledExecutor para browser
        ScheduledExecutor.BEAN.set(new ScheduledExecutorBrowser());

        // Configura ClientStorage para browser (sessionStorage)
        var storage = new BrowserSessionStorage();
        ClientStorage.BEAN.set(storage);

        // Configura HTTP transport e registra repositórios
        HttpTransport transport = new FetchHttpTransport(apiBaseUrl);
        TeaVMRepositoryBootstrap.initialize(transport, storage);

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
            // Força inicialização do enum Routes.Place (registra GoActions no mapa)
            Routes.Place.values();
            var hash = getLocationHash();
            if (hash != null && hash.startsWith("#") && hash.length() > 1) {
                // No boot, o secret é novo (F5 gera outro), então strip assinatura antiga se houver
                var location = intentSigner.stripSignature(hash.substring(1));
                try {
                    this.go(location);
                } catch (Exception e) {
                    LOG.warn("Failed to navigate to hash '" + location + "': " + e.getMessage());
                    Routes.root(this);
                }
            } else {
                Routes.root(this);
            }
        }).start();
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.attributeMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributeMap.remove(name);
    }

    @Override
    public void updateHistory() {
        // Mark all active views as dirty for UI refresh
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewTeaVM<?> teavmView) {
                    this.markDirty(teavmView);
                }
            }
        }

        // Sync browser URL hash with signed intent
        if (!navigatingFromBrowser) {
            try {
                var intent = this.newIntent();
                var intentStr = intent.toString();
                this.fragment = intentStr;
                var signedUrl = intentSigner.sign(intentStr);
                pushState("#" + signedUrl);
            } catch (Exception e) {
                LOG.error("Failed to push browser history state: " + e.getMessage());
            }
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
