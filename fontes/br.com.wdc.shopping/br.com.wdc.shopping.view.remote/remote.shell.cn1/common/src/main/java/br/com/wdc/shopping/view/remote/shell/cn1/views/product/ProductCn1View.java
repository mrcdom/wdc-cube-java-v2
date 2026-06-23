package br.com.wdc.shopping.view.remote.shell.cn1.views.product;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.HtmlText;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.QtyStepper;

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
    // Dimensões em mm (densidade-independente) — ver util.Px.
    private static final float DETAIL_MM = 28f;
    private static final float DIVIDER_H_MM = 0.6f;
    /** Lado (mm) dos botões +/- de quantidade — evita a altura mínima gigante do Button. */
    private static final float QTY_BTN_MM = 8f;

    private Consumer<String> name;
    private Consumer<String> price;
    private Label image;
    private QtyStepper stepper;
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
            dom.label(l -> {                
                l.setUIID(sel.PRODUCT_TITLE);
                name = Guard.text(l);
            });
            Label divider = dom.label(l -> l.setUIID(sel.PRODUCT_DIVIDER));
            divider.setPreferredH(Px.mm(DIVIDER_H_MM));

            // card de descrição
            dom.boxY(card -> {
                card.setUIID(sel.PRODUCT_DESC_CARD);
                description = dom.add(new HtmlText(sel.PRODUCT_DESC_TEXT), null);
            });

            // preço/quantidade + imagem — lado a lado no expandido; empilhado (imagem em cima) no
            // compacto, senão a soma das larguras estoura a tela do telefone e a imagem fica cortada.
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, row -> {
                row.setUIID(sel.PRODUCT_PRICE_IMAGE_ROW);
                if (app.isExpanded()) {
                    dom.boxX(group -> {
                        buildPriceQty(dom);
                        buildImageBox(dom);
                    });
                } else {
                    dom.boxY(stack -> {
                        buildImageBox(dom);
                        buildPriceQty(dom);
                    });
                }
            });

            // ações: Voltar + Adicionar ao Carrinho
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                dom.add(new BackButton("Voltar", () -> submit(EVT_BACK)), null);
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText("Adicionar ao Carrinho");
                    b.addActionListener(e -> addToCart());
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_ADD_SHOPPING_CART, 3.5f);
                });
            });
        });
        updateQty();
        return root;
    }

    /** Caixa da imagem do produto (fundo gradiente), centralizada. */
    private void buildImageBox(Cn1Dom dom) {
        dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, box -> {
            box.setUIID(sel.PRODUCT_IMAGE_BOX);
            image = dom.label(l -> { });
        });
    }

    /** Badge de preço + linha "Qtd: − valor +". O qty usa BoxLayout.x (centraliza na vertical, como o
     *  stepper do carrinho); o FlowLayout externo o centraliza na horizontal. */
    private void buildPriceQty(Cn1Dom dom) {
        dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, priceWrap -> {
            dom.boxY(priceCol -> {
                dom.label(l -> {
                    l.setUIID(sel.PRODUCT_PRICE_BADGE);
                    price = Guard.text(l);
                });                
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, qtyWrap -> {
                    dom.boxX(qtyRow -> {
                        dom.label(l -> {
                            l.setUIID(sel.QTY_LABEL);
                            l.setText("Qtd:");
                        });
                        stepper = dom.add(new QtyStepper(QTY_BTN_MM,
                                () -> changeQty(-1), () -> changeQty(1)), null);
                    });
                });
            });
        });
    }

    private void changeQty(int delta) {
        int next = quantity + delta;
        if (next >= 1) {
            quantity = next;
            updateQty();
        }
    }

    private void updateQty() {
        stepper.setValue(quantity);
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
            image.setIcon(Images.product(id, Px.mm(DETAIL_MM)));
        }
        name.accept(Json.str(p, "name"));
        price.accept(Money.format(Json.doubleOf(p, "price")));
        String html = Json.str(p, "description");
        if (!html.equals(lastHtml)) {
            lastHtml = html;
            description.setHtml(html);
        }
    }
}
