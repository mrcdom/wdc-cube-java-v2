package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.RootCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.cart.CartCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.home.HomeCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.home.ProductsPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.login.LoginCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.product.ProductCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt.ReceiptCn1View;

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
    private final Map<String, AbstractCn1View> views = new HashMap<>();
    private final Map<String, Function<String, AbstractCn1View>> registry = new HashMap<>();
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
        registry.put(RootCn1View.CLASS_ID, vsid -> new RootCn1View(vsid, session, this));
        registry.put(LoginCn1View.CLASS_ID, vsid -> new LoginCn1View(vsid, session, this));
        registry.put(HomeCn1View.CLASS_ID, vsid -> new HomeCn1View(vsid, session, this));
        registry.put(ProductsPanelCn1View.CLASS_ID, vsid -> new ProductsPanelCn1View(vsid, session, this));
        registry.put(ProductCn1View.CLASS_ID, vsid -> new ProductCn1View(vsid, session, this));
        registry.put(CartCn1View.CLASS_ID, vsid -> new CartCn1View(vsid, session, this));
        registry.put(ReceiptCn1View.CLASS_ID, vsid -> new ReceiptCn1View(vsid, session, this));
    }

    /** Retorna (criando sob demanda via registry) a view de um vsid; {@code null} se classId desconhecido. */
    public AbstractCn1View viewFor(String vsid) {
        if (vsid == null || vsid.isEmpty()) {
            return null;
        }
        AbstractCn1View v = views.get(vsid);
        if (v == null) {
            Function<String, AbstractCn1View> factory = registry.get(BridgeSession.classIdOf(vsid));
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
            AbstractCn1View root = viewFor(rv);
            form.removeAll();
            if (root != null) {
                form.add(BorderLayout.CENTER, root.getElement());
            }
        }
        AbstractCn1View root = views.get(rootVsid);
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
