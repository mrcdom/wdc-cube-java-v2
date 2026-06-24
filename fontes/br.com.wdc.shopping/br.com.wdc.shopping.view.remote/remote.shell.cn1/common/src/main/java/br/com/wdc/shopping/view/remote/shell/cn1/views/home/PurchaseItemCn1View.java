package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Clickable;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Dates;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;

/** Item do histórico: linha 1 (#id + data) e linha 2 (itens + total); o card abre o recibo. */
public class PurchaseItemCn1View extends AbstractItemCn1View<Object> {

    private static final HomeSel sel = HomeSel.INSTANCE;

    private final Consumer<Long> onOpen;
    private Consumer<String> idLabel;
    private Consumer<String> dateLabel;
    private Consumer<String> itemsLabel;
    private Consumer<String> totalLabel;
    private long currentId = -1;

    public PurchaseItemCn1View(Consumer<Long> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    protected Container build() {
        Container content = Cn1Dom.render(BoxLayout.y(), (dom, c) -> {
            dom.border(line1 -> {
                line1.setUIID(sel.PURCHASE_LINE);
                dom.label(BorderLayout.WEST, l -> {
                    l.setUIID(sel.PURCHASE_ID);
                    idLabel = Guard.text(l);
                });
                dom.label(BorderLayout.EAST, l -> {
                    l.setUIID(sel.PURCHASE_DATE);
                    dateLabel = Guard.text(l);
                });
            });
            dom.border(line2 -> {
                line2.setUIID(sel.PURCHASE_LINE);
                dom.label(BorderLayout.CENTER, l -> {
                    l.setUIID(sel.PURCHASE_ITEMS);
                    l.setEndsWith3Points(true); // elipsis quando não couber
                    itemsLabel = Guard.text(l);
                });
                dom.label(BorderLayout.EAST, l -> {
                    l.setUIID(sel.PURCHASE_TOTAL);
                    totalLabel = Guard.text(l);
                });
            });
        });
        return Clickable.card(sel.PURCHASE_CARD, content, () -> onOpen.accept(currentId));
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        idLabel.accept("#" + currentId);
        dateLabel.accept(Dates.formatDate(Json.longOf(m, "date")));
        itemsLabel.accept(joinItems(Json.asList(m.get("items"))));
        totalLabel.accept(Money.format(Json.doubleOf(m, "total")));
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
