package br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Dates;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Widgets;

/**
 * Recibo da compra (classId {@value #CLASS_ID}) — espelha o React: alerta verde de sucesso, card com
 * cabeçalho (ícone + título) e o corpo em fonte monoespaçada com a tabela (Data, ITEM/QTD/VALOR,
 * itens e TOTAL), além do link "Voltar aos produtos".
 */
public class ReceiptCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "e8d0bd8ae3bc";
    private static final int EVT_BACK = 1;

    /** Larguras (px) das colunas QTD e VALOR — compartilhadas com {@link ReceiptItemCn1View}. */
    static final int COL_QTY_W = 130;
    static final int COL_VALUE_W = 220;
    /** Fonte monoespaçada do corpo do recibo (estilo cupom). */
    static final Font MONO = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

    private Container successAlert;
    private Label dateValue;
    private Label total;
    private Container list;
    private final List<ReceiptItemCn1View> items = new ArrayList<>();

    public ReceiptCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        root.setUIID("ReceiptPage");
        Cn1Dom.render(root, (dom, r) -> {
            // alerta verde de sucesso
            successAlert = dom.boxX(alert -> {
                alert.setUIID("AlertSuccess");
                Label icon = dom.label(l -> l.setUIID("AlertSuccessIcon"));
                FontImage.setMaterialIcon(icon, FontImage.MATERIAL_CHECK_CIRCLE, 4f);
                dom.label(l -> {
                    l.setText("Compra realizada com sucesso!");
                    l.setUIID("AlertSuccessText");
                });
            });

            // card do recibo
            dom.boxY(card -> {
                card.setUIID("ReceiptCard");

                // cabeçalho: ícone + título/subtítulo
                dom.boxX(header -> {
                    header.setUIID("ReceiptHeaderRow");
                    Label icon = dom.label(l -> l.setUIID("ReceiptIconBox"));
                    FontImage.setMaterialIcon(icon, FontImage.MATERIAL_RECEIPT, 4.5f);
                    dom.boxY(titleCol -> {
                        dom.label(l -> {
                            l.setText("Recibo de Compra");
                            l.setUIID("ReceiptCardTitle");
                        });
                        dom.label(l -> {
                            l.setText("WDC Shopping");
                            l.setUIID("ReceiptCardSub");
                        });
                    });
                });

                // corpo (monoespaçado)
                dom.boxY(body -> {
                    body.setUIID("ReceiptBody");

                    // data
                    dom.border(dateRow -> {
                        dateRow.setUIID("ReceiptDateRow");
                        Label lbl = dom.label(BorderLayout.WEST, l -> l.setUIID("ReceiptDateLabel"));
                        lbl.setText("Data:");
                        mono(lbl);
                        dateValue = dom.label(BorderLayout.EAST, l -> l.setUIID("ReceiptDateValue"));
                        mono(dateValue);
                    });

                    // cabeçalho da tabela
                    dom.border(th -> {
                        th.setUIID("ReceiptTableHeader");
                        Label item = dom.label(BorderLayout.CENTER, l -> l.setUIID("ReceiptColHead"));
                        item.setText("ITEM");
                        mono(item);
                        dom.boxX(BorderLayout.EAST, cols -> {
                            Label qtd = dom.label(l -> l.setUIID("ReceiptColHeadQty"));
                            qtd.setText("QTD");
                            qtd.setPreferredW(COL_QTY_W);
                            mono(qtd);
                            Label val = dom.label(l -> l.setUIID("ReceiptColHeadValue"));
                            val.setText("VALOR");
                            val.setPreferredW(COL_VALUE_W);
                            mono(val);
                        });
                    });

                    // itens
                    list = dom.boxY(l -> { });

                    // total
                    dom.border(totalRow -> {
                        totalRow.setUIID("ReceiptTotalRow");
                        Label lbl = dom.label(BorderLayout.WEST, l -> l.setUIID("ReceiptTotalLabel"));
                        lbl.setText("TOTAL:");
                        mono(lbl);
                        total = dom.label(BorderLayout.EAST, l -> l.setUIID("ReceiptTotalValue"));
                        mono(total);
                    });
                });

                // voltar
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), null, actions -> {
                    dom.add(Widgets.backButton("Voltar aos produtos", () -> submit(EVT_BACK)), null);
                });
            });
        });
        return root;
    }

    /** Aplica a fonte monoespaçada do cupom. */
    static void mono(Label l) {
        l.getAllStyles().setFont(MONO);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        visible(successAlert, Json.boolOf(st, "notifySuccess"));

        Map<String, Object> receipt = Json.asMap(st.get("receipt"));
        dateValue.setText(Dates.formatDateTime(Json.longOf(receipt, "date")));
        total.setText("R$ " + money(receipt, "total"));

        List<Object> receiptItems = Json.asList(receipt != null ? receipt.get("items") : null);
        syncList(list, receiptItems, items, ReceiptItemCn1View::new);
    }

    private static String money(Map<String, Object> m, String key) {
        return Money.format(Json.doubleOf(m, key)).replace("R$ ", "");
    }
}
