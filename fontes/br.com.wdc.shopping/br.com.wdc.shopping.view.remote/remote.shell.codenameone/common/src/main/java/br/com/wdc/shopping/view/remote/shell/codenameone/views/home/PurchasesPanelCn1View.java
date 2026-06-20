package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.util.UITimer;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Painel de histórico de compras (classId {@value #CLASS_ID}) — cabeçalho, lista paginada de compras
 * (cada item abre o recibo) e navegação de páginas. O servidor só carrega os dados depois que a view
 * informa quantos itens cabem na altura disponível ({@value #EVT_PAGE_SIZE}); essa medição é refeita
 * a cada redimensionamento e a notificação é <b>debounced</b> para não inundar o backend.
 */
public class PurchasesPanelCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "b3c4d5e6f7a8";
    private static final int EVT_OPEN_RECEIPT = 1;
    private static final int EVT_PAGE_CHANGE = 2;
    private static final int EVT_PAGE_SIZE = 3;

    /** Altura estimada (px) de um item antes de medir o primeiro renderizado. */
    private static final int DEFAULT_ITEM_H = 110;
    /** Janela do debounce da notificação de capacidade (ms). */
    private static final int DEBOUNCE_MS = 250;

    /** Tamanho (mm) e cores do ícone das setas (cor é "assada" na criação do ícone). */
    private static final float CHEVRON_MM = 2.8f;
    private static final int CHEVRON_ENABLED = 0x6e6e73;
    private static final int CHEVRON_DISABLED = 0xc7c7cc;
    private static final int CHEVRON_PRESSED = 0x0d66d0;

    private Container list;
    private Container pagination;
    private Container pill;
    private Label pageInfo;
    private Label prevBtn;
    private Label nextBtn;
    private final List<PurchaseItemCn1View> items = new ArrayList<>();

    private int totalPages = 1;
    private int lastCapacity = -1;
    private int capacityToken;
    private boolean resizeHooked;
    /** Altura travada das setas (= altura do box); re-aplicada após cada re-bake do ícone. */
    private int chevronH;

    public PurchasesPanelCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(new BorderLayout());
        root.setUIID("PurchasesPanel");
        Cn1Dom.render(root, (dom, r) -> {
            // cabeçalho (ícone + título) + dica
            dom.boxY(BorderLayout.NORTH, head -> {
                dom.boxX(row -> {
                    Label icon = dom.label(l -> l.setUIID("PurchasesHeaderIcon"));
                    FontImage.setMaterialIcon(icon, FontImage.MATERIAL_HISTORY, 4f);
                    dom.label(l -> {
                        l.setText("Histórico");
                        l.setUIID("PurchasesTitle");
                    });
                });
                dom.label(l -> {
                    l.setText("Toque para ver detalhes");
                    l.setUIID("PurchasesHint");
                });
            });

            // lista de compras (rolável, ocupa o espaço restante)
            list = dom.boxY(BorderLayout.CENTER, l -> l.setScrollableY(true));

            // navegação de páginas — pílula cinza com setas e o "x / y" num box branco (como o React)
            pagination = dom.container(new FlowLayout(Component.CENTER), BorderLayout.SOUTH, nav -> {
                nav.setUIID("PurchasePagination");
                pill = dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, p -> {
                    p.setUIID("PurchasePagePill");
                    prevBtn = dom.label(l -> {
                        l.setUIID("PurchasePageBtn");
                        l.addPointerPressedListener(e -> chevronIcon(l, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_PRESSED));
                        l.addPointerReleasedListener(e -> {
                            chevronIcon(l, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_ENABLED);
                            changePage(-1);
                        });
                    });
                    chevronIcon(prevBtn, FontImage.MATERIAL_CHEVRON_LEFT, CHEVRON_ENABLED);
                    pageInfo = dom.label(l -> l.setUIID("PurchasePageInfo"));
                    nextBtn = dom.label(l -> {
                        l.setUIID("PurchasePageBtn");
                        l.addPointerPressedListener(e -> chevronIcon(l, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_PRESSED));
                        l.addPointerReleasedListener(e -> {
                            chevronIcon(l, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_ENABLED);
                            changePage(1);
                        });
                    });
                    chevronIcon(nextBtn, FontImage.MATERIAL_CHEVRON_RIGHT, CHEVRON_ENABLED);
                });
            });
        });
        stylePaginator();
        return root;
    }

    /**
     * Acabamento do box "x / y" que o CSS do CN1 não cobre: cantos arredondados + uma sombra
     * <b>sutil</b> (box-shadow não existe no CSS do CN1) via {@link RoundRectBorder}, para o box
     * "flutuar" na pílula como no React.
     */
    private void stylePaginator() {
        // sombra discreta no box "x / y"
        RoundRectBorder infoBorder = RoundRectBorder.create()
                .cornerRadius(2.5f)
                .shadowOpacity(45)
                .shadowSpread(0f)
                .shadowBlur(0.8f)
                .shadowY(0.4f);
        pageInfo.getAllStyles().setBorder(infoBorder);
        pageInfo.getAllStyles().setBgColor(0xffffff);
        pageInfo.getAllStyles().setBgTransparency(255);

        // Pílula arredondada via RoundRectBorder do Java (o border-radius grande do CSS reservava
        // espaço vertical extra — o cinza que sobrava). Mesmo mecanismo do box, que fica justo.
        pill.getAllStyles().setBorder(RoundRectBorder.create().cornerRadius(2.5f));
        pill.getAllStyles().setBgColor(0xf4f6f9);
        pill.getAllStyles().setBgTransparency(255);

        // Setas com EXATAMENTE a altura do box: assim a pílula = altura do box (sem cinza
        // sobrando) e as setas ficam centralizadas verticalmente com o indicador. A altura
        // natural de um Label/Button é maior que a do box (reserva da fonte/toque), por isso
        // travamos a altura aqui, mantendo a largura natural do ícone.
        chevronH = pageInfo.getPreferredH();
        lockChevronHeight(prevBtn);
        lockChevronHeight(nextBtn);
    }

    /** Trava a altura da seta na altura do box (setIcon marca p/ recalcular, então re-aplicamos). */
    private void lockChevronHeight(Label l) {
        if (chevronH > 0) {
            l.setPreferredSize(new Dimension(l.getPreferredW(), chevronH));
        }
    }

    /** (Re)gera o ícone da seta com a cor indicada — a cor é "assada" na criação do FontImage. */
    private void chevronIcon(Label l, char icon, int color) {
        l.getAllStyles().setFgColor(color);
        FontImage.setMaterialIcon(l, icon, CHEVRON_MM);
        lockChevronHeight(l); // setIcon marcou p/ recalcular; mantém a altura travada (sem cinza)
        l.repaint();
    }

    /** Habilita/desabilita a seta: cinza-claro + sem clique quando não há para onde ir. */
    private void setChevronEnabled(Label l, char icon, boolean enabled) {
        l.setEnabled(enabled);
        chevronIcon(l, icon, enabled ? CHEVRON_ENABLED : CHEVRON_DISABLED);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        List<Object> purchases = Json.asList(st.get("purchases"));
        syncList(list, purchases, items, () -> new PurchaseItemCn1View(this::openReceipt));

        int page = Json.intOf(st, "page");
        int pageSize = Math.max(1, Json.intOf(st, "pageSize"));
        int totalCount = Json.intOf(st, "totalCount");
        totalPages = Math.max(1, (totalCount + pageSize - 1) / pageSize);

        pageInfo.setText((page + 1) + " / " + totalPages);
        setChevronEnabled(prevBtn, FontImage.MATERIAL_CHEVRON_LEFT, page > 0);
        setChevronEnabled(nextBtn, FontImage.MATERIAL_CHEVRON_RIGHT, page < totalPages - 1);
        visible(pagination, totalCount > 0);

        hookResize();
        requestCapacityCompute();
    }

    /** Recalcula a capacidade quando o form muda de tamanho (registra o listener uma vez). */
    private void hookResize() {
        if (resizeHooked) {
            return;
        }
        Form f = list.getComponentForm();
        if (f != null) {
            f.addSizeChangedListener(e -> requestCapacityCompute());
            resizeHooked = true;
        }
    }

    /** Agenda a medição/notificação com debounce — só a última agendada dentro da janela executa. */
    private void requestCapacityCompute() {
        Form f = list.getComponentForm();
        if (f == null) {
            return;
        }
        final int my = ++capacityToken;
        new UITimer(() -> {
            if (my == capacityToken) {
                computeCapacityNow();
            }
        }).schedule(DEBOUNCE_MS, false, f);
    }

    /** Quantos itens cabem sem rolar — mede a lista e o 1º item, como o painel React. */
    private void computeCapacityNow() {
        int h = list.getHeight();
        if (h <= 0) {
            return; // ainda sem layout; um próximo doUpdate/resize reagenda
        }
        // passo real entre itens (inclui a margem) — evita contar um item a mais
        int itemH = DEFAULT_ITEM_H;
        int n = list.getComponentCount();
        if (n >= 2) {
            int stride = list.getComponentAt(1).getY() - list.getComponentAt(0).getY();
            if (stride > 0) {
                itemH = stride;
            }
        } else if (n == 1) {
            int h0 = list.getComponentAt(0).getHeight();
            if (h0 > 0) {
                itemH = h0;
            }
        }
        int capacity = Math.max(1, h / itemH);
        if (capacity != lastCapacity) {
            lastCapacity = capacity;
            Map<String, Object> form = new HashMap<>();
            form.put("p.capacity", capacity);
            submit(EVT_PAGE_SIZE, form);
        }
    }

    private void changePage(int delta) {
        int page = Json.intOf(state(), "page");
        int target = page + delta;
        if (target < 0 || target > totalPages - 1) {
            return;
        }
        Map<String, Object> form = new HashMap<>();
        form.put("p.page", target);
        submit(EVT_PAGE_CHANGE, form);
    }

    private void openReceipt(long purchaseId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.purchaseId", purchaseId);
        submit(EVT_OPEN_RECEIPT, form);
    }
}
