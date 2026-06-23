package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.theme.Fonts;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Dates;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;

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

    private Container successAlert;
    private Consumer<String> dateValue;
    private Consumer<String> total;
    private Container list;
    private final List<ReceiptItemCn1View> items = new ArrayList<>();

    public ReceiptCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.RECEIPT_PAGE);
            r.setScrollableY(true);

            // alerta verde de sucesso
            dom.boxX(alert -> {
                alert.setUIID(sel.ALERT_SUCCESS);
                dom.label(l -> {
                    l.setUIID(sel.ALERT_SUCCESS_ICON);
                    FontImage.setMaterialIcon(l, FontImage.MATERIAL_CHECK_CIRCLE, 4f);
                });
                dom.label(l -> {
                    l.setUIID(sel.ALERT_SUCCESS_TEXT);
                    l.setText("Compra realizada com sucesso!");
                });
                
                successAlert = alert;
            });

            // card do recibo
            dom.boxY(card -> {
                card.setUIID(sel.RECEIPT_CARD);

                dom.cardHeader(null, h -> {
                    h.setIcon(FontImage.MATERIAL_RECEIPT);
                    h.setTitle("Recibo de Compra");
                    h.setSubtitle("WDC Shopping");
                });

                // corpo (monoespaçado)
                dom.boxY(body -> {
                    body.setUIID(sel.RECEIPT_BODY);

                    // data
                    dom.border(dateRow -> {
                        dateRow.setUIID(sel.RECEIPT_DATE_ROW);
                        dom.label(BorderLayout.WEST, l -> {
                            l.setUIID(sel.RECEIPT_DATE_LABEL);
                            l.setText("Data:");
                            l.getAllStyles().setFont(Fonts.MONO);    
                        });
                        dom.label(BorderLayout.EAST, l -> {
                            l.setUIID(sel.RECEIPT_DATE_VALUE);
                            l.getAllStyles().setFont(Fonts.MONO);
                            dateValue = Guard.text(l);
                        });
                    });

                    // cabeçalho da tabela
                    dom.border(th -> {
                        th.setUIID(sel.RECEIPT_TABLE_HEADER);
                        dom.label(BorderLayout.CENTER, l -> {
                            l.setUIID(sel.RECEIPT_COL_HEAD);
                            l.setText("ITEM");
                            l.getAllStyles().setFont(Fonts.MONO);
                        });
                        dom.boxX(BorderLayout.EAST, cols -> {
                            dom.label(l -> {
                                l.setUIID(sel.RECEIPT_COL_HEAD_QTY);
                                l.setText("QTD");
                                l.setPreferredW(Px.mm(COL_QTY_W_MM));
                                l.getAllStyles().setFont(Fonts.MONO);    
                            });                            
                            dom.label(l -> {
                                l.setUIID(sel.RECEIPT_COL_HEAD_VALUE);
                                l.setPreferredW(Px.mm(COL_VALUE_W_MM));
                                l.getAllStyles().setFont(Fonts.MONO);
                                l.setText("VALOR");
                            });
                        });
                    });

                    // itens
                    list = dom.boxY(Cn1Dom.NO_CONTENT);

                    // total
                    dom.border(totalRow -> {
                        totalRow.setUIID(sel.RECEIPT_TOTAL_ROW);
                        Label lbl = dom.label(BorderLayout.WEST, l -> l.setUIID(sel.RECEIPT_TOTAL_LABEL));
                        lbl.setText("TOTAL:");
                        lbl.getAllStyles().setFont(Fonts.MONO);
                        dom.label(BorderLayout.EAST, l -> {
                            l.setUIID(sel.RECEIPT_TOTAL_VALUE);
                            l.getAllStyles().setFont(Fonts.MONO);
                            total = Guard.text(l);
                        });
                    });
                });

                // voltar
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), null, actions -> {
                    dom.add(new BackButton("Voltar aos produtos", () -> submit(EVT_BACK)), null);
                });
            });
        });
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        Guard.visible(successAlert, Json.boolOf(st, "notifySuccess"));

        Map<String, Object> receipt = Json.asMap(st.get("receipt"));
        dateValue.accept(Dates.formatDateTime(Json.longOf(receipt, "date")));
        total.accept("R$ " + money(receipt, "total"));

        List<Object> receiptItems = Json.asList(receipt != null ? receipt.get("items") : null);
        syncList(list, receiptItems, items, ReceiptItemCn1View::new);
    }

    private static String money(Map<String, Object> m, String key) {
        return Money.format(Json.doubleOf(m, key)).replace("R$ ", "");
    }
}
