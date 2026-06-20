package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Widgets;

/**
 * Carrinho (classId {@value #CLASS_ID}) — espelha o React: card com cabeçalho e, conforme o estado,
 * o <b>estado vazio</b> (ícone redondo + "Carrinho vazio" + "Ver produtos") ou a <b>lista de itens</b>
 * (nome | stepper | subtotal | remover), com total e ações (Continuar comprando / Finalizar pedido).
 */
public class CartCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "7eb485e5f843";
    private static final int EVT_BUY = 1;
    private static final int EVT_REMOVE = 2;
    private static final int EVT_BACK = 3;
    private static final int EVT_MODIFY = 4;

    private Container body;
    private Container emptySection;
    private Container itemsSection;
    private Container list;
    private Label total;
    private Boolean mountedEmpty;
    private final List<CartItemCn1View> items = new ArrayList<>();

    public CartCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        root.setUIID("CartPage");
        Cn1Dom.render(root, (dom, r) -> dom.boxY(card -> {
            card.setUIID("CartCard");
            dom.add(Widgets.cardHeader(FontImage.MATERIAL_SHOPPING_BAG, "Carrinho",
                    "Seus produtos selecionados"), null);
            body = dom.boxY(b -> { });
        }));
        emptySection = buildEmpty();
        itemsSection = buildItems();
        return root;
    }

    private Container buildEmpty() {
        Container sec = new Container(BoxLayout.y());
        sec.setUIID("CartEmpty");
        Cn1Dom.render(sec, (dom, r) -> {
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                Label iconBox = dom.label(l -> l.setUIID("CartEmptyIconBox"));
                iconBox.setPreferredSize(new Dimension(150, 150));
                iconBox.getAllStyles().setBorder(RoundBorder.create().color(0xe8f1fc));
                FontImage.setMaterialIcon(iconBox, FontImage.MATERIAL_SHOPPING_BAG, 8f);
            });
            dom.label(l -> {
                l.setText("Carrinho vazio");
                l.setUIID("CartEmptyTitle");
            });
            dom.label(l -> {
                l.setText("Adicione produtos para começar");
                l.setUIID("CartEmptySub");
            });
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                Button view = dom.button(b -> {
                    b.setText("Ver produtos");
                    b.setUIID("PrimaryButton");
                    b.addActionListener(e -> submit(EVT_BACK));
                });
                FontImage.setMaterialIcon(view, FontImage.MATERIAL_VIEW_MODULE, 3.5f);
            });
        });
        return sec;
    }

    private Container buildItems() {
        Container sec = new Container(BoxLayout.y());
        Cn1Dom.render(sec, (dom, r) -> {
            list = dom.boxY(l -> { });
            dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), null, footer -> {
                footer.setUIID("CartFooter");
                dom.label(l -> {
                    l.setText("Total:");
                    l.setUIID("CartFooterLabel");
                });
                total = dom.label(l -> l.setUIID("CartFooterTotal"));
            });
            dom.border(actions -> {
                actions.setUIID("CartActions");
                dom.add(Widgets.backButton("Continuar comprando", () -> submit(EVT_BACK)), BorderLayout.WEST);
                Button buy = dom.button(BorderLayout.EAST, b -> {
                    b.setText("Finalizar pedido");
                    b.setUIID("PrimaryButton");
                    b.addActionListener(e -> submit(EVT_BUY));
                });
                FontImage.setMaterialIcon(buy, FontImage.MATERIAL_CHECK_CIRCLE, 3.5f);
            });
        });
        return sec;
    }

    @Override
    public void doUpdate() {
        List<Object> cartItems = Json.asList(state().get("items"));
        boolean empty = cartItems.isEmpty();

        if (mountedEmpty == null || mountedEmpty.booleanValue() != empty) {
            mountedEmpty = Boolean.valueOf(empty);
            body.removeAll();
            body.add(empty ? emptySection : itemsSection);
            body.revalidate();
        }

        if (!empty) {
            syncList(list, cartItems, items,
                    () -> new CartItemCn1View(this::modifyQuantity, this::removeProduct));
            double sum = 0;
            for (Object o : cartItems) {
                Map<String, Object> m = Json.asMap(o);
                sum += Json.doubleOf(m, "price") * Json.intOf(m, "quantity");
            }
            total.setText(Money.format(sum));
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
