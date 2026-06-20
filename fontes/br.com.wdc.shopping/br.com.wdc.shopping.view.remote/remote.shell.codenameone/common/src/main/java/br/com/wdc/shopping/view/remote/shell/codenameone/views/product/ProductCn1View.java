package br.com.wdc.shopping.view.remote.shell.codenameone.views.product;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.SimpleHtml;

/**
 * Detalhe do produto (classId {@value #CLASS_ID}) — espelha o design React: título + divisória,
 * card de descrição, linha com preço/quantidade à esquerda e imagem à direita, e a barra de ações
 * (Voltar / Adicionar ao Carrinho). A quantidade é controlada localmente (stepper − valor +).
 */
public class ProductCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "48b693f67410";
    private static final int EVT_BACK = 1;
    private static final int EVT_ADD_TO_CART = 2;
    private static final int DETAIL_PX = 200;
    private static final int DIVIDER_H = 4;

    private Label name;
    private Label price;
    private Label image;
    private Label qtyValue;
    private Container description;
    private long currentId = -1;
    private String lastHtml = "";
    private int quantity = 1;

    public ProductCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        root.setUIID("ProductPage");
        Cn1Dom.render(root, (dom, r) -> {
            name = dom.label(l -> l.setUIID("ProductTitle"));
            Label divider = dom.label(l -> l.setUIID("ProductDivider"));
            divider.setPreferredH(DIVIDER_H);

            // card de descrição
            dom.boxY(card -> {
                card.setUIID("ProductDescCard");
                description = dom.boxY(c -> { });
            });

            // preço/quantidade (esquerda) + imagem (direita)
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, row -> {
                row.setUIID("ProductPriceImageRow");
                dom.boxY(priceCol -> {
                    price = dom.label(l -> l.setUIID("ProductPriceBadge"));
                    dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, qtyRow -> {
                        dom.label(l -> {
                            l.setText("Qtd:");
                            l.setUIID("QtyLabel");
                        });
                        stepBtn(dom, FontImage.MATERIAL_REMOVE, -1);
                        qtyValue = dom.label(l -> l.setUIID("QtyValue"));
                        stepBtn(dom, FontImage.MATERIAL_ADD, 1);
                    });
                });
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, box -> {
                    box.setUIID("ProductImageBox");
                    image = dom.label(l -> { });
                });
            });

            // ações: Voltar + Adicionar ao Carrinho
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                Button back = dom.button(b -> {
                    b.setText("Voltar");
                    b.setUIID("LinkButton");
                    b.addActionListener(e -> submit(EVT_BACK));
                });
                FontImage.setMaterialIcon(back, FontImage.MATERIAL_ARROW_BACK, 3.5f);
                Button add = dom.button(b -> {
                    b.setText("Adicionar ao Carrinho");
                    b.setUIID("PrimaryButton");
                    b.addActionListener(e -> addToCart());
                });
                FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD_SHOPPING_CART, 3.5f);
            });
        });
        updateQty();
        return root;
    }

    private void stepBtn(Cn1Dom dom, char icon, int delta) {
        Button b = dom.button(bt -> {
            bt.setUIID("QtyBtn");
            bt.addActionListener(e -> changeQty(delta));
        });
        FontImage.setMaterialIcon(b, icon, 3.5f);
    }

    private void changeQty(int delta) {
        int next = quantity + delta;
        if (next >= 1) {
            quantity = next;
            updateQty();
        }
    }

    private void updateQty() {
        qtyValue.setText(String.valueOf(quantity));
    }

    private void addToCart() {
        Map<String, Object> form = new HashMap<>();
        form.put("p.quantity", quantity);
        submit(EVT_ADD_TO_CART, form);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> p = Json.asMap(state().get("product"));
        long id = Json.longOf(p, "id");
        if (id != currentId) {
            currentId = id;
            image.setIcon(Images.product(id, DETAIL_PX));
        }
        name.setText(Json.str(p, "name"));
        price.setText(Money.format(Json.doubleOf(p, "price")));
        String html = Json.str(p, "description");
        if (!html.equals(lastHtml)) {
            lastHtml = html;
            SimpleHtml.render(description, html, "ProductDescText");
        }
    }
}
