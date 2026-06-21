package br.com.wdc.shopping.view.remote.shell.cn1;

import java.util.HashMap;
import java.util.Map;

import com.codename1.components.InfiniteProgress;
import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Decor;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.views.RootCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.cart.CartCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.HomeCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.ProductsPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.PurchasesPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.login.LoginCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.product.ProductCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.receipt.ReceiptCn1View;

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

    private static final Sel sel = Sel.INSTANCE;

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
        form.addSizeChangedListener(e -> onResize());
        form.show();

        startConnect();
    }

    /** Mostra a tela de "conectando" e abre o bridge numa thread; em falha, mostra erro + retry. */
    private void startConnect() {
        showSplash(false, "Conectando ao servidor...", null);
        session = new BridgeSession(BASE, s -> flush());
        new Thread(() -> {
            try {
                session.connect();
            } catch (Exception e) {
                CN.callSerially(() -> showSplash(true, "Não foi possível conectar ao servidor.", this::startConnect));
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
            case PurchasesPanelCn1View.CLASS_ID:
                return new PurchasesPanelCn1View(vsid, session, this);
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

    /**
     * Re-sincroniza a árvore ativa (no EDT). Monta a raiz quando o browser aponta para outra e, em
     * seguida, <b>despacha {@code doUpdate()} para cada ViewState recebido</b> no último push (as
     * views "dirty" do servidor). Cada view cuida do próprio dado e da <i>montagem</i> das suas
     * filhas; não há propagação manual de {@code doUpdate} pelos pais (cf. shells React/TeaVM).
     */
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
            for (String vsid : session.lastReceived()) {
                AbstractCn1View v = views.get(vsid);
                if (v != null) {
                    v.doUpdate();
                }
            }
            form.revalidate();
        } catch (Exception e) {
            com.codename1.io.Log.e(e);
            showSplash(true, "Erro ao renderizar a tela.", null);
        }
    }

    /**
     * Tela de "marca" exibida antes de receber o estado do backend (e em caso de falha): gradiente
     * azul + logo + título, com um <b>spinner</b> (conectando) ou um ícone de erro + "Tentar novamente".
     */
    private void showSplash(boolean error, String status, Runnable onRetry) {
        Container root = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        Decor.blueWithCircles(root);

        Container content = new Container(BoxLayout.y());

        Label logo = new Label();
        logo.getAllStyles().setFgColor(0xffffff);
        FontImage.setMaterialIcon(logo, error ? FontImage.MATERIAL_CLOUD_OFF : FontImage.MATERIAL_SHOPPING_BAG, 13f);
        content.add(FlowLayout.encloseCenter(logo));

        Label title = new Label("WDC Shopping");
        title.setUIID(sel.HERO_TITLE);
        content.add(title);

        SpanLabel subtitle = new SpanLabel("Sua compra certa na internet.");
        subtitle.setTextUIID(sel.HERO_SUBTITLE);
        content.add(subtitle);

        if (!error) {
            InfiniteProgress spinner = new InfiniteProgress();
            spinner.setMaterialDesignMode(true);
            spinner.getAllStyles().setFgColor(0xffffff);
            content.add(FlowLayout.encloseCenter(spinner));
        }

        Label statusLabel = new Label(status);
        statusLabel.setUIID(sel.SPLASH_STATUS);
        content.add(FlowLayout.encloseCenter(statusLabel));

        if (onRetry != null) {
            Button retry = new Button("Tentar novamente");
            retry.setUIID(sel.SPLASH_RETRY);
            retry.addActionListener(e -> onRetry.run());
            content.add(FlowLayout.encloseCenter(retry));
        }

        root.add(content);
        form.removeAll();
        form.add(BorderLayout.CENTER, root);
        form.revalidate();
    }
}
