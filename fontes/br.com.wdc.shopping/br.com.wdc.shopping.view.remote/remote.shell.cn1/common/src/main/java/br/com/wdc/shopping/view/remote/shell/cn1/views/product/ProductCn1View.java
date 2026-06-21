package br.com.wdc.shopping.view.remote.shell.cn1.views.product;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.HtmlText;

/**
 * Detalhe do produto (classId {@value #CLASS_ID}) — espelha o design React: título + divisória,
 * card de descrição, linha com preço/quantidade à esquerda e imagem à direita, e a barra de ações
 * (Voltar / Adicionar ao Carrinho). A quantidade é controlada localmente (stepper − valor +).
 */
public class ProductCn1View extends AbstractCn1View {

    private static final ProductSel sel = ProductSel.INSTANCE;

    public static final String CLASS_ID = "48b693f67410";
    private static final int EVT_BACK = 1;
    private static final int EVT_ADD_TO_CART = 2;
    private static final int DETAIL_PX = 200;
    private static final int DIVIDER_H = 4;
    /** Lado (px) dos botões +/- de quantidade — evita a altura mínima gigante do Button. */
    private static final int QTY_BTN = 58;

    private Label name;
    private Label price;
    private Label image;
    private Label qtyValue;
    private HtmlText description;
    private long currentId = -1;
    private String lastHtml = "";
    private int quantity = 1;

    public ProductCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setScrollableY(true);
            r.setUIID(sel.PRODUCT_PAGE);
            name = dom.label(l -> l.setUIID(sel.PRODUCT_TITLE));
            Label divider = dom.label(l -> l.setUIID(sel.PRODUCT_DIVIDER));
            divider.setPreferredH(DIVIDER_H);

            // card de descrição
            dom.boxY(card -> {
                card.setUIID(sel.PRODUCT_DESC_CARD);
                description = dom.add(new HtmlText(sel.PRODUCT_DESC_TEXT), null);
            });

            // preço/quantidade (esquerda) + imagem (direita), alinhados pelo centro vertical
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, row -> {
                row.setUIID(sel.PRODUCT_PRICE_IMAGE_ROW);
                // BoxLayout.x estica os dois wraps à mesma altura; cada wrap centraliza o conteúdo
                dom.boxX(group -> {
                    dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, priceWrap -> {
                        dom.boxY(priceCol -> {
                            price = dom.label(l -> l.setUIID(sel.PRODUCT_PRICE_BADGE));
                            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, qtyRow -> {
                                dom.label(l -> {
                                    l.setText("Qtd:");
                                    l.setUIID(sel.QTY_LABEL);
                                });
                                stepBtn(dom, FontImage.MATERIAL_REMOVE, -1);
                                qtyValue = dom.label(l -> l.setUIID(sel.QTY_VALUE));
                                stepBtn(dom, FontImage.MATERIAL_ADD, 1);
                            });
                        });
                    });
                    dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, box -> {
                        box.setUIID(sel.PRODUCT_IMAGE_BOX);
                        image = dom.label(l -> { });
                    });
                });
            });

            // ações: Voltar + Adicionar ao Carrinho
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                dom.add(new BackButton("Voltar", () -> submit(EVT_BACK)), null);
                Button add = dom.button(b -> {
                    b.setText("Adicionar ao Carrinho");
                    b.setUIID(sel.PRIMARY_BUTTON);
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
            bt.setUIID(sel.QTY_BTN);
            bt.addActionListener(e -> changeQty(delta));
        });
        FontImage.setMaterialIcon(b, icon, 3f);
        b.setPreferredSize(new Dimension(QTY_BTN, QTY_BTN)); // trava o tamanho (sem o mínimo de toque)
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
            description.setHtml(html);
        }
    }
}
