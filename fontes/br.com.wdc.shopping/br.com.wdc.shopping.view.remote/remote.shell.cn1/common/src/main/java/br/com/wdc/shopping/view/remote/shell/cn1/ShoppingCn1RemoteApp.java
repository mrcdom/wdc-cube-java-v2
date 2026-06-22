package br.com.wdc.shopping.view.remote.shell.cn1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Largura mínima (em mm) para o layout expandido — densidade-independente (tablet/desktop). */
    private static final float EXPANDED_MIN_MM = 110f;

    private BridgeSession session;
    private Form form;
    /** URL base do backend, resolvida por plataforma/override no {@code runApp} (ver {@link ServerConfig}). */
    private String baseUrl;
    private final Map<String, AbstractCn1View> views = new HashMap<>();
    /** Views a re-renderizar no próximo flush (primitivo único, como o {@code dirtyViews} do SWT). */
    private final Set<AbstractCn1View> dirty = new LinkedHashSet<>();
    private String rootVsid = "";
    private boolean flushScheduled;
    private boolean wasExpanded;
    private boolean wasPortrait;

    /**
     * Layout expandido (hero+card, split produtos+histórico) em <b>tablet/desktop</b>; compacto no
     * telefone. Largura sozinha não basta: um telefone em <b>paisagem</b> (~145mm) ultrapassaria
     * qualquer limiar e viraria "expandido", quebrando o split numa tela de telefone. Por isso o sinal
     * de forma é {@code isTablet()}/{@code isDesktop()}; o limiar em mm fica só para o resize no desktop.
     */
    public boolean isExpanded() {
        Display d = Display.getInstance();
        if (d.isTablet()) {
            return true; // tablet (iPad): sempre expandido
        }
        if (d.isDesktop()) {
            return d.getDisplayWidth() >= d.convertToPixels(EXPANDED_MIN_MM); // desktop: por largura (resize)
        }
        return false; // telefone: compacto nas duas orientações (paisagem não é tablet)
    }

    /**
     * Resize. Reconstrói ao <b>cruzar o breakpoint</b> (estrutura abas↔colunas, decidida em
     * {@code build()}) <b>ou ao girar a tela</b> (orientação) — neste último o refluxo automático do
     * CN1 deixa estado obsoleto (sizing dos cards, safe area da orientação anterior) e só um rebuild
     * conserta (igual ao logout/login). Dentro do mesmo breakpoint+orientação não fazemos nada: o CN1
     * relayouta sozinho e os painéis avisam o backend pelo seu próprio {@code sizeChangedListener}.
     */
    private void onResize() {
        boolean nowExpanded = isExpanded();
        boolean nowPortrait = Display.getInstance().isPortrait();
        if (nowExpanded == wasExpanded && nowPortrait == wasPortrait) {
            return;
        }
        wasExpanded = nowExpanded;
        wasPortrait = nowPortrait;
        views.clear();
        dirty.clear();
        rootVsid = "";
        form.removeAll();
        mountRootIfChanged(); // recria a árvore ativa (getElement → build → doUpdate) no novo tamanho
        form.setSafeAreaChanged(); // recomputa a safe area para a nova orientação
        form.revalidate();
    }

    @Override
    public void runApp() {
        baseUrl = ServerConfig.baseUrl();
        Images.setBaseUrl(baseUrl);
        wasExpanded = isExpanded();
        wasPortrait = Display.getInstance().isPortrait();

        form = new Form(new BorderLayout());
        form.getToolbar().hideToolbar(); // sem barra de título nativa; cada tela traz seu header
        // iOS (notch/Dynamic Island): sem toolbar, o CN1 não reserva a status bar e o header colaria
        // sob ela (toques iriam para o iOS). Mantém o conteúdo dentro da safe area.
        form.getContentPane().setSafeArea(true);
        form.addSizeChangedListener(e -> onResize());
        form.show();

        startConnect();
    }

    /** Mostra a tela de "conectando" e abre o bridge numa thread; em falha, mostra erro + retry. */
    private void startConnect() {
        showSplash(false, "Conectando ao servidor...", null);
        session = new BridgeSession(baseUrl, this::onPush);
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

    /**
     * Recebe um push do servidor (no EDT): descarta as views liberadas, monta a raiz se mudou e
     * marca <b>dirty</b> cada ViewState recebido — espelha o {@code applyResponse} do shell SWT.
     * O render efetivo é feito pelo {@link #flush()} coalescido.
     */
    private void onPush(List<String> received, List<String> released) {
        for (String vsid : released) {
            evict(vsid);
        }
        mountRootIfChanged();
        for (String vsid : received) {
            AbstractCn1View v = views.get(vsid); // já criada/montada (criação é lazy, via o pai)
            if (v != null) {
                dirty.add(v);
            }
        }
        requestFlush();
    }

    /** Descarta uma view liberada pelo servidor: tira do cache, do dirty e desmonta (evita vazamento). */
    private void evict(String vsid) {
        AbstractCn1View v = views.remove(vsid);
        if (v == null) {
            return;
        }
        dirty.remove(v);
        com.codename1.ui.Container el = v.peekElement();
        if (el != null && el.getParent() != null) {
            el.getParent().removeComponent(el);
        }
    }

    /** Monta a view raiz no form quando o browser passa a apontar para outra. */
    private void mountRootIfChanged() {
        Map<String, Object> browser = session.state(BridgeSession.BROWSER_VSID);
        String rv = browser != null ? Json.str(browser, "contentViewId") : "";
        if (rv.isEmpty() || rv.equals(rootVsid)) {
            return;
        }
        rootVsid = rv;
        AbstractCn1View root = viewFor(rv);
        form.removeAll();
        if (root != null) {
            form.add(BorderLayout.CENTER, root.getElement());
        }
    }

    /** Agenda re-render de uma view (primitivo único — push do servidor e interações locais). */
    public void markDirty(AbstractCn1View view) {
        if (view != null) {
            dirty.add(view);
            requestFlush();
        }
    }

    /** Agenda um flush coalescido. */
    public void requestFlush() {
        if (!flushScheduled) {
            flushScheduled = true;
            CN.callSerially(this::flush);
        }
    }

    /**
     * Render coalescido (no EDT): drena o conjunto {@code dirty} e chama {@code doUpdate()} em cada
     * view. Cada view cuida do próprio dado e da <i>montagem</i> das suas filhas; não há propagação
     * manual de {@code doUpdate} pelos pais (cf. shells React/TeaVM/SWT).
     */
    private void flush() {
        flushScheduled = false;
        try {
            List<AbstractCn1View> batch = new ArrayList<>(dirty);
            dirty.clear();
            for (AbstractCn1View v : batch) {
                v.doUpdate();
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
