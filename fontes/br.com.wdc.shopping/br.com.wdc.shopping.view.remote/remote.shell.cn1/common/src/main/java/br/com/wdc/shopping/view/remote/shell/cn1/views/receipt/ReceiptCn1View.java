package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

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

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Dates;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.CardHeader;

/**
 * Recibo da compra (classId {@value #CLASS_ID}) — espelha o React: alerta verde de sucesso, card com
 * cabeçalho (ícone + título) e o corpo em fonte monoespaçada com a tabela (Data, ITEM/QTD/VALOR,
 * itens e TOTAL), além do link "Voltar aos produtos".
 */
public class ReceiptCn1View extends AbstractCn1View {

    private static final ReceiptSel sel = ReceiptSel.INSTANCE;

    public static final String CLASS_ID = "e8d0bd8ae3bc";
    private static final int EVT_BACK = 1;

    /** Larguras (mm) das colunas QTD e VALOR — compartilhadas com {@link ReceiptItemCn1View}. */
    static final float COL_QTY_W_MM = 12f;
    static final float COL_VALUE_W_MM = 22f;
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
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setScrollableY(true);
            r.setUIID(sel.RECEIPT_PAGE);
            // alerta verde de sucesso
            successAlert = dom.boxX(alert -> {
                alert.setUIID(sel.ALERT_SUCCESS);
                Label icon = dom.label(l -> l.setUIID(sel.ALERT_SUCCESS_ICON));
                FontImage.setMaterialIcon(icon, FontImage.MATERIAL_CHECK_CIRCLE, 4f);
                dom.label(l -> {
                    l.setText("Compra realizada com sucesso!");
                    l.setUIID(sel.ALERT_SUCCESS_TEXT);
                });
            });

            // card do recibo
            dom.boxY(card -> {
                card.setUIID(sel.RECEIPT_CARD);

                // cabeçalho: ícone + título/subtítulo (widget compartilhado)
                dom.add(new CardHeader(FontImage.MATERIAL_RECEIPT, "Recibo de Compra", "WDC Shopping"), null);

                // corpo (monoespaçado)
                dom.boxY(body -> {
                    body.setUIID(sel.RECEIPT_BODY);

                    // data
                    dom.border(dateRow -> {
                        dateRow.setUIID(sel.RECEIPT_DATE_ROW);
                        Label lbl = dom.label(BorderLayout.WEST, l -> l.setUIID(sel.RECEIPT_DATE_LABEL));
                        lbl.setText("Data:");
                        mono(lbl);
                        dateValue = dom.label(BorderLayout.EAST, l -> l.setUIID(sel.RECEIPT_DATE_VALUE));
                        mono(dateValue);
                    });

                    // cabeçalho da tabela
                    dom.border(th -> {
                        th.setUIID(sel.RECEIPT_TABLE_HEADER);
                        Label item = dom.label(BorderLayout.CENTER, l -> l.setUIID(sel.RECEIPT_COL_HEAD));
                        item.setText("ITEM");
                        mono(item);
                        dom.boxX(BorderLayout.EAST, cols -> {
                            Label qtd = dom.label(l -> l.setUIID(sel.RECEIPT_COL_HEAD_QTY));
                            qtd.setText("QTD");
                            qtd.setPreferredW(Px.mm(COL_QTY_W_MM));
                            mono(qtd);
                            Label val = dom.label(l -> l.setUIID(sel.RECEIPT_COL_HEAD_VALUE));
                            val.setText("VALOR");
                            val.setPreferredW(Px.mm(COL_VALUE_W_MM));
                            mono(val);
                        });
                    });

                    // itens
                    list = dom.boxY(l -> { });

                    // total
                    dom.border(totalRow -> {
                        totalRow.setUIID(sel.RECEIPT_TOTAL_ROW);
                        Label lbl = dom.label(BorderLayout.WEST, l -> l.setUIID(sel.RECEIPT_TOTAL_LABEL));
                        lbl.setText("TOTAL:");
                        mono(lbl);
                        total = dom.label(BorderLayout.EAST, l -> l.setUIID(sel.RECEIPT_TOTAL_VALUE));
                        mono(total);
                    });
                });

                // voltar
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), null, actions -> {
                    dom.add(new BackButton("Voltar aos produtos", () -> submit(EVT_BACK)), null);
                });
            });
        });
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
