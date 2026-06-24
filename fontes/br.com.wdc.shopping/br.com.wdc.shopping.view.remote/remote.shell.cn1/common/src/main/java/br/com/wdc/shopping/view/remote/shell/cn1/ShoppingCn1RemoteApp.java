package br.com.wdc.shopping.view.remote.shell.cn1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.views.RootCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.SplashCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.cart.CartCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.HomeCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.ProductsPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.home.PurchasesPanelCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.login.LoginCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.product.ProductCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.views.receipt.ReceiptCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

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

    /** Largura mínima (mm) que o layout expandido precisa; abaixo disso, compacto (densidade-independente). */
    private static final float EXPANDED_MIN_MM = 110f;

    private BridgeSession session;
    private Form form;
    /** URL base do backend, resolvida por plataforma/override no {@code runApp} (ver {@link ServerConfig}). */
    private String baseUrl;
    private final Map<String, AbstractCn1View> views = new HashMap<>();
    /** Views a re-renderizar no próximo flush (primitivo único, como o {@code dirtyViews} do SWT). */
    private final Set<AbstractCn1View> dirty = new LinkedHashSet<>();
    /** Único filho do form: acomoda a view raiz (e o splash) — guard de "montar só na transição". */
    private Slot rootSlot;
    /** Splash (conexão/erro) — construído uma vez, reusado via setState. */
    private SplashCn1View splash;
    private boolean flushScheduled;
    private boolean wasExpanded;
    private boolean wasPortrait;

    /**
     * Layout expandido (hero+card, split produtos+histórico) quando a <b>largura disponível</b> comporta
     * o espaço que o expandido precisa ({@code >= EXPANDED_MIN_MM}); senão, compacto. Critério puramente
     * por <b>largura</b> (mm, densidade-independente) — independe de device (tablet/desktop/telefone) e de
     * orientação: o que importa é caber. Ex.: telefone em paisagem largo o bastante fica expandido;
     * estreito, compacto. (O limiar é o tamanho a ajustar se o split ficar apertado.)
     */
    public boolean isExpanded() {
        Display d = Display.getInstance();
        return d.getDisplayWidth() >= d.convertToPixels(EXPANDED_MIN_MM);
    }

    /**
     * Resize. Ao <b>cruzar o breakpoint</b> ou <b>girar a tela</b>, re-sincroniza tudo via
     * {@link #refreshAll()}: cada view adapta o próprio layout no {@code doUpdate()} (sem rebuild),
     * preservando o estado (ex.: texto digitado). Recomputa a safe area da nova orientação antes do
     * {@code revalidate} (feito pelo {@code refreshAll}). Dentro do mesmo breakpoint+orientação não faz
     * nada: o CN1 relayouta sozinho e os painéis avisam o backend pelo seu próprio
     * {@code sizeChangedListener}.
     */
    private void onResize() {
        boolean nowExpanded = isExpanded();
        boolean nowPortrait = Display.getInstance().isPortrait();
        if (nowExpanded == wasExpanded && nowPortrait == wasPortrait) {
            return;
        }
        wasExpanded = nowExpanded;
        wasPortrait = nowPortrait;
        form.setSafeAreaChanged(); // recomputa a safe area para a nova orientação (antes do revalidate)
        refreshAll();
    }

    @Override
    public void runApp() {
        baseUrl = ServerConfig.baseUrl();
        Images.setBaseUrl(baseUrl);
        wasExpanded = isExpanded();
        wasPortrait = Display.getInstance().isPortrait();

        form = new Form(new BorderLayout());
        // iOS (notch/Dynamic Island): sem toolbar, o CN1 não reserva a status bar e o header colaria
        // sob ela (toques iriam para o iOS). Mantém o conteúdo dentro da safe area.
        form.getContentPane().setSafeArea(true);
        rootSlot = new Slot();
        form.add(BorderLayout.CENTER, rootSlot); // filho único e permanente; sobrevive ao resize
        form.addSizeChangedListener(e -> onResize());
        form.show();
        // hideToolbar() exige o form já associado (getComponentForm()): no desktop/javase ele é null
        // antes do show() (no device não). Como runApp roda no EDT e o paint vem depois, não pisca.
        form.getToolbar().hideToolbar(); // sem barra de título nativa; cada tela traz seu header

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

    /** Monta a view raiz no slot quando o browser passa a apontar para outra (guard interno do slot). */
    private void mountRootIfChanged() {
        Map<String, Object> browser = session.state(BridgeSession.BROWSER_VSID);
        String rv = browser != null ? Json.str(browser, "contentViewId") : "";
        if (rv.isEmpty()) {
            return; // push sem contentViewId não desmonta a raiz atual
        }
        rootSlot.mount(elementOf(rv)); // re-monta só quando a referência do element muda (mesmo vsid → mesma view)
    }

    /** Element da view de {@code vsid} (criada sob demanda), ou {@code null}. */
    private Container elementOf(String vsid) {
        AbstractCn1View v = viewFor(vsid);
        return v != null ? v.getElement() : null;
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
     * Reconcilia <b>todas</b> as views montadas com o estado atual: roda {@code doUpdate()} em cada view
     * do cache (vsid-backed) — que por sua vez re-sincroniza seus item-views via {@code syncList} — e no
     * splash. Use para um re-render completo (ex.: após reconectar). O resize <b>não</b> usa isto: lá a
     * estrutura muda (compacto↔expandido) e é preciso reconstruir, não só re-sincronizar.
     *
     * <p>Itera um <b>snapshot</b> de {@code views}: o {@code doUpdate} de uma raiz pode criar/cachear
     * novas views ({@code viewFor}) e mutar o mapa durante a iteração.</p>
     */
    public void refreshAll() {
        try {
            for (AbstractCn1View v : new ArrayList<>(views.values())) {
                v.doUpdate();
            }
            if (splash != null) {
                splash.refresh();
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
        if (splash == null) {
            splash = new SplashCn1View(); // construída uma vez; reusada (build-once + setState)
        }
        splash.setState(new SplashCn1View.State(error, status, onRetry));
        rootSlot.mount(splash.getElement()); // mesma referência sempre → só re-monta vindo da raiz
    }
}
