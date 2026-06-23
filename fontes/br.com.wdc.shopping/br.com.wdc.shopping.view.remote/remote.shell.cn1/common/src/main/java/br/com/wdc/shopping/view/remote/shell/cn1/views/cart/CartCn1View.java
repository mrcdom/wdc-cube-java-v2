package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Carrinho (classId {@value #CLASS_ID}) — espelha o React: card com cabeçalho e, conforme o estado,
 * o <b>estado vazio</b> (ícone redondo + "Carrinho vazio" + "Ver produtos") ou a <b>lista de itens</b>
 * (nome | stepper | subtotal | remover), com total e ações (Continuar comprando / Finalizar pedido).
 */
public class CartCn1View extends AbstractCn1View {

    private static final CartSel sel = CartSel.INSTANCE;

    public static final String CLASS_ID = "7eb485e5f843";
    private static final int EVT_BUY = 1;
    private static final int EVT_REMOVE = 2;
    private static final int EVT_BACK = 3;
    private static final int EVT_MODIFY = 4;

    /** Lado (mm) do ícone redondo do estado vazio — densidade-independente (ver util.Px). */
    private static final float EMPTY_ICON_MM = 20f;

    private Slot body;
    private Container emptySection;
    private Container itemsSection;
    private Container list;
    private Consumer<String> total;
    private final List<CartItemCn1View> items = new ArrayList<>();

    public CartCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_PAGE);
            r.setScrollableY(true);
            dom.boxY(card -> {
                card.setUIID(sel.CART_CARD);
                
                dom.cardHeader(null, h -> {
                    h.setIcon(FontImage.MATERIAL_SHOPPING_BAG);
                    h.setTitle("Carrinho");
                    h.setSubtitle("Seus produtos selecionados");
                });

                body = dom.slot();
            });
        });
        emptySection = buildEmpty();
        itemsSection = buildItems();
        return root;
    }

    private Container buildEmpty() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_EMPTY);
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                dom.label(l -> {
                    l.setUIID(sel.CART_EMPTY_ICON_BOX);
                    l.setPreferredSize(new Dimension(Px.mm(EMPTY_ICON_MM), Px.mm(EMPTY_ICON_MM)));
                    l.getAllStyles().setBorder(RoundBorder.create().color(Colors.ACCENT_LIGHT));
                    FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 8f);
                });
            });
            dom.label(l -> {
                l.setUIID(sel.CART_EMPTY_TITLE);
                l.setText("Carrinho vazio");
            });
            dom.label(l -> {
                l.setUIID(sel.CART_EMPTY_SUB);
                l.setText("Adicione produtos para começar");
            });
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText("Ver produtos");
                    b.addActionListener(e -> submit(EVT_BACK));
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_VIEW_MODULE, 3.5f);
                });
            });
        });
    }

    private Container buildItems() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            list = dom.boxY(Cn1Dom.NO_CONTENT);
            dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), null, footer -> {
                footer.setUIID(sel.CART_FOOTER);
                dom.label(l -> {
                    l.setUIID(sel.CART_FOOTER_LABEL);
                    l.setText("Total:");
                });
                dom.label(l -> {
                    l.setUIID(sel.CART_FOOTER_TOTAL);
                    total = Guard.text(l);
                });
            });
            // ações em FlowLayout centralizado: no compacto quebram a linha (empilham) em vez de cortar
            // o texto; no expandido ficam lado a lado. Mesmo padrão das ações do ProductCn1View.
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                actions.setUIID(sel.CART_ACTIONS);
                dom.add(new BackButton("Continuar comprando", () -> submit(EVT_BACK)), null);
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText("Finalizar pedido");
                    b.addActionListener(e -> submit(EVT_BUY));
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_CHECK_CIRCLE, 3.5f);
                });
            });
        });
    }

    @Override
    public void doUpdate() {
        List<Object> cartItems = Json.asList(state().get("items"));
        boolean empty = cartItems.isEmpty();

        body.mount(empty ? emptySection : itemsSection); // troca só na transição (comparação de referência)

        if (!empty) {
            boolean expanded = app.isExpanded();
            syncList(list, cartItems, items,
                    () -> new CartItemCn1View(expanded, this::modifyQuantity, this::removeProduct));
            double sum = 0;
            for (Object o : cartItems) {
                Map<String, Object> m = Json.asMap(o);
                sum += Json.doubleOf(m, "price") * Json.intOf(m, "quantity");
            }
            total.accept(Money.format(sum));
        }
    }

    private void modifyQuantity(long productId, int quantity) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        form.put("p.quantity", quantity);
        submit(EVT_MODIFY, form);
    }

    private void removeProduct(long productId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        submit(EVT_REMOVE, form);
    }
}
