package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Clickable;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Dates;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;

/** Item do histórico: linha 1 (#id + data) e linha 2 (itens + total); o card abre o recibo. */
public class PurchaseItemCn1View extends AbstractItemCn1View<Object> {

    private static final HomeSel sel = HomeSel.INSTANCE;

    private final Consumer<Long> onOpen;
    private Label idLabel;
    private Label dateLabel;
    private Label itemsLabel;
    private Label totalLabel;
    private long currentId = -1;

    public PurchaseItemCn1View(Consumer<Long> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    protected Container build() {
        Container content = new Container(BoxLayout.y());
        Cn1Dom.render(content, (dom, c) -> {
            dom.border(line1 -> {
                line1.setUIID(sel.PURCHASE_LINE);
                idLabel = dom.label(BorderLayout.WEST, l -> l.setUIID(sel.PURCHASE_ID));
                dateLabel = dom.label(BorderLayout.EAST, l -> l.setUIID(sel.PURCHASE_DATE));
            });
            dom.border(line2 -> {
                line2.setUIID(sel.PURCHASE_LINE);
                itemsLabel = dom.label(BorderLayout.CENTER, l -> {
                    l.setUIID(sel.PURCHASE_ITEMS);
                    l.setEndsWith3Points(true); // elipsis quando não couber
                });
                totalLabel = dom.label(BorderLayout.EAST, l -> l.setUIID(sel.PURCHASE_TOTAL));
            });
        });
        return Clickable.card(sel.PURCHASE_CARD, content, () -> onOpen.accept(currentId));
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        idLabel.setText("#" + currentId);
        dateLabel.setText(Dates.formatDate(Json.longOf(m, "date")));
        itemsLabel.setText(joinItems(Json.asList(m.get("items"))));
        totalLabel.setText(Money.format(Json.doubleOf(m, "total")));
    }

    private static String joinItems(List<Object> items) {
        StringBuilder sb = new StringBuilder();
        for (Object o : items) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(o);
        }
        return sb.toString();
    }
}
