package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
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

    private Container list;
    private Container pagination;
    private Label pageInfo;
    private Button prevBtn;
    private Button nextBtn;
    private final List<PurchaseItemCn1View> items = new ArrayList<>();

    private int totalPages = 1;
    private int lastCapacity = -1;
    private int capacityToken;
    private boolean resizeHooked;

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
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, pill -> {
                    pill.setUIID("PurchasePagePill");
                    prevBtn = dom.button(b -> {
                        b.setUIID("PurchasePageBtn");
                        b.addActionListener(e -> changePage(-1));
                    });
                    FontImage.setMaterialIcon(prevBtn, FontImage.MATERIAL_CHEVRON_LEFT, 3f);
                    pageInfo = dom.label(l -> l.setUIID("PurchasePageInfo"));
                    nextBtn = dom.button(b -> {
                        b.setUIID("PurchasePageBtn");
                        b.addActionListener(e -> changePage(1));
                    });
                    FontImage.setMaterialIcon(nextBtn, FontImage.MATERIAL_CHEVRON_RIGHT, 3f);
                });
            });
        });
        return root;
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
        prevBtn.setEnabled(page > 0);
        nextBtn.setEnabled(page < totalPages - 1);
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
