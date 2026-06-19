package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.HashMap;
import java.util.Map;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Display;
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
    private String rootVsid = "";
    private boolean flushScheduled;
    private boolean wasExpanded;

    /** Layout expandido (hero+card etc.) quando a largura é grande; senão compacto. */
    public boolean isExpanded() {
        return Display.getInstance().getDisplayWidth() >= 1100;
    }

    /** Ao cruzar o breakpoint de largura, descarta as views e reconstrói no novo tamanho. */
    private void onResize() {
        boolean now = isExpanded();
        if (now != wasExpanded) {
            wasExpanded = now;
            views.clear();
            rootVsid = "";
            form.removeAll();
            flush();
        }
    }

    @Override
    public void runApp() {
        Images.setBaseUrl(BASE);
        wasExpanded = isExpanded();

        form = new Form(new BorderLayout());
        form.getToolbar().hideToolbar(); // sem barra de título nativa; cada tela traz seu header
        form.add(BorderLayout.CENTER, new SpanLabel("Conectando ao servidor..."));
        form.addSizeChangedListener(e -> onResize());
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

    /** Retorna (criando sob demanda) a view de um vsid; {@code null} se classId desconhecido. */
    public AbstractCn1View viewFor(String vsid) {
        if (vsid == null || vsid.isEmpty()) {
            return null;
        }
        AbstractCn1View v = views.get(vsid);
        if (v == null) {
            v = create(vsid);
            if (v != null) {
                views.put(vsid, v);
            }
        }
        return v;
    }

    /** Instancia a view do classId (switch direto — sem reflection nem lambda-factory). */
    private AbstractCn1View create(String vsid) {
        String classId = BridgeSession.classIdOf(vsid);
        switch (classId) {
            case RootCn1View.CLASS_ID:
                return new RootCn1View(vsid, session, this);
            case LoginCn1View.CLASS_ID:
                return new LoginCn1View(vsid, session, this);
            case HomeCn1View.CLASS_ID:
                return new HomeCn1View(vsid, session, this);
            case ProductsPanelCn1View.CLASS_ID:
                return new ProductsPanelCn1View(vsid, session, this);
            case ProductCn1View.CLASS_ID:
                return new ProductCn1View(vsid, session, this);
            case CartCn1View.CLASS_ID:
                return new CartCn1View(vsid, session, this);
            case ReceiptCn1View.CLASS_ID:
                return new ReceiptCn1View(vsid, session, this);
            default:
                com.codename1.io.Log.p("CN1 shell: sem view para classId=" + classId + " (vsid=" + vsid + ")");
                return null;
        }
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
        try {
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
        } catch (Exception e) {
            com.codename1.io.Log.e(e);
            showStatus("Erro ao renderizar: " + e);
        }
    }

    private void showStatus(String text) {
        form.removeAll();
        form.add(BorderLayout.CENTER, new SpanLabel(text));
        form.revalidate();
    }
}
