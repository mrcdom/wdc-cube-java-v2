package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Dates;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Item do histórico: linha 1 (#id + data) e linha 2 (itens + total); o card abre o recibo. */
public class PurchaseItemCn1View extends AbstractItemCn1View<Object> {

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
        Container card = new Container(BoxLayout.y());
        card.setUIID("PurchaseCard");
        Cn1Dom.render(card, (dom, c) -> {
            dom.border(line1 -> {
                line1.setUIID("PurchaseLine");
                idLabel = dom.label(BorderLayout.WEST, l -> l.setUIID("PurchaseId"));
                dateLabel = dom.label(BorderLayout.EAST, l -> l.setUIID("PurchaseDate"));
            });
            dom.border(line2 -> {
                line2.setUIID("PurchaseLine");
                itemsLabel = dom.label(BorderLayout.CENTER, l -> l.setUIID("PurchaseItems"));
                totalLabel = dom.label(BorderLayout.EAST, l -> l.setUIID("PurchaseTotal"));
            });
        });
        card.addPointerReleasedListener(e -> onOpen.accept(currentId));
        return card;
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
