package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.RootViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.cart.CartViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.home.HomeViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.home.ProductsPanelViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.login.LoginViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.product.ProductViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt.ReceiptViewCn1;

/**
 * Shell fino Codename One (remote-shell) — coordenador.
 *
 * <p>
 * Conecta no bridge ({@link BridgeSession}), mantém um registry classId→factory e um cache
 * vsid→view, e a cada push <b>re-sincroniza a árvore ativa</b> em cascata a partir da raiz. Cada
 * view é uma classe própria que constrói os widgets uma vez e os muta em {@code doUpdate()} (padrão
 * não-reativo, estilo Vaadin) — nada é reconstruído por push.
 * </p>
 */
public class ShoppingCn1RemoteApp extends Lifecycle {

    private static final String BASE = "http://localhost:8080";

    private BridgeSession session;
    private Form form;
    private final Map<String, AbstractViewCn1> views = new HashMap<>();
    private final Map<String, Function<String, AbstractViewCn1>> registry = new HashMap<>();
    private String rootVsid = "";
    private boolean flushScheduled;

    @Override
    public void runApp() {
        Images.setBaseUrl(BASE);
        registerViews();

        form = new Form("WDC Shopping", new BorderLayout());
        form.add(BorderLayout.CENTER, new SpanLabel("Conectando ao servidor..."));
        form.show();

        session = new BridgeSession(BASE, s -> flush());
        new Thread(() -> {
            try {
                session.connect();
            } catch (Exception e) {
                CN.callSerially(() -> showStatus("Falha ao conectar: " + e.getMessage()));
            }
        }).start();
    }

    private void registerViews() {
        registry.put(RootViewCn1.CLASS_ID, vsid -> new RootViewCn1(vsid, session, this));
        registry.put(LoginViewCn1.CLASS_ID, vsid -> new LoginViewCn1(vsid, session, this));
        registry.put(HomeViewCn1.CLASS_ID, vsid -> new HomeViewCn1(vsid, session, this));
        registry.put(ProductsPanelViewCn1.CLASS_ID, vsid -> new ProductsPanelViewCn1(vsid, session, this));
        registry.put(ProductViewCn1.CLASS_ID, vsid -> new ProductViewCn1(vsid, session, this));
        registry.put(CartViewCn1.CLASS_ID, vsid -> new CartViewCn1(vsid, session, this));
        registry.put(ReceiptViewCn1.CLASS_ID, vsid -> new ReceiptViewCn1(vsid, session, this));
    }

    /** Retorna (criando sob demanda via registry) a view de um vsid; {@code null} se classId desconhecido. */
    public AbstractViewCn1 viewFor(String vsid) {
        if (vsid == null || vsid.isEmpty()) {
            return null;
        }
        AbstractViewCn1 v = views.get(vsid);
        if (v == null) {
            Function<String, AbstractViewCn1> factory = registry.get(BridgeSession.classIdOf(vsid));
            if (factory == null) {
                return null;
            }
            v = factory.apply(vsid);
            views.put(vsid, v);
        }
        return v;
    }

    /** Agenda um flush coalescido (chamado por views após interação local). */
    public void requestFlush() {
        if (!flushScheduled) {
            flushScheduled = true;
            CN.callSerially(() -> {
                flushScheduled = false;
                flush();
            });
        }
    }

    /** Re-sincroniza a árvore ativa a partir do estado do bridge (no EDT). */
    private void flush() {
        Map<String, Object> browser = session.state(BridgeSession.BROWSER_VSID);
        String rv = browser != null ? Json.str(browser, "contentViewId") : "";
        if (rv.isEmpty()) {
            return; // ainda sem estado
        }
        if (!rv.equals(rootVsid)) {
            rootVsid = rv;
            AbstractViewCn1 root = viewFor(rv);
            form.removeAll();
            if (root != null) {
                form.add(BorderLayout.CENTER, root.getElement());
            }
        }
        AbstractViewCn1 root = views.get(rootVsid);
        if (root != null) {
            root.doUpdate();
        }
        form.revalidate();
    }

    private void showStatus(String text) {
        form.removeAll();
        form.add(BorderLayout.CENTER, new SpanLabel(text));
        form.revalidate();
    }
}
