package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

/**
 * Purchases history panel with pagination. State: purchases (array of {id, date, items, total}), page, totalCount,
 * pageSize.
 */
public class PurchasesPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "b3c4d5e6f7a8";

    private static final int ON_OPEN_RECEIPT = 1;
    private static final int ON_PAGE_CHANGE = 2;
    private static final int ON_PAGE_SIZE_CHANGED = 3;
    private static final int ITEM_HEIGHT_PX = 56;

    @SuppressWarnings({ "java:S1214", "static-access" })
    private interface Sel {
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = "purchases-panel";
        String HEADER_ROW = "purchases-header-row";
        String HEADER_ICON = clsx(icon.CLOCK_HISTORY, "purchases-header-icon");
        String HEADER_TITLE = "purchases-header-title";
        String HINT = "purchases-hint";
        String LIST_CONTAINER = "purchases-list-container";
        String PAGINATION = "purchases-pagination";
        String PAGE_PILL = "purchases-page-pill";
        String PAGE_BTN = "purchases-page-btn";
        String PAGE_PREV_ICON = clsx(icon.CHEVRON_LEFT, "purchases-page-btn-icon");
        String PAGE_NEXT_ICON = clsx(icon.CHEVRON_RIGHT, "purchases-page-btn-icon");
        String PAGE_INFO = "purchases-page-info";
        String ITEM_CARD = clsx("purchase-item", "purchases-item-card");
        String ITEM_LINE1 = "purchases-item-line1";
        String ITEM_ID = "purchases-item-id";
        String ITEM_DATE = "purchases-item-date";
        String ITEM_LINE2 = "purchases-item-line2";
        String ITEM_ITEMS = "purchases-item-items";
        String ITEM_TOTAL = "purchases-item-total";
    }

    private HTMLElement listContainer;
    private int pendingResizeFrame = -1;

    private record Purchase(Object id, String key, String idStr, String date, String items, String total) {
    }

    private EventListener<Event> mkOnOpenReceipt(Object id) {
        return evt -> {
            setFormField("p.purchaseId", id);
            submit(ON_OPEN_RECEIPT);
        };
    }

    private EventListener<Event> mkOnPageChange(int page) {
        return evt -> changePage(page);
    }

    public PurchasesPanelView(String vsid) {
        super(vsid);
        Window.current().addEventListener("resize", evt -> scheduleResize());
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        var scope = state();
        int pageSize = scope.getInt("pageSize");
        if (pageSize <= 0) {
            Window.requestAnimationFrame(t -> computePageSize());
        }
    }

    @Override
    protected VNode render() {
        var scope = state();
        var purchases = getPurchases(scope);
        int page = scope.getInt("page");
        int totalCount = scope.getInt("totalCount");
        int pageSize = Math.max(1, scope.getInt("pageSize"));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        var pageInfo = (page + 1) + " / " + totalPages;

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.HEADER_ROW).children(
            span(Sel.HEADER_ICON),
            span(Sel.HEADER_TITLE).text("Histórico")),
          span(Sel.HINT).text("Toque para ver detalhes"),
          div(Sel.LIST_CONTAINER)
            .ref(el -> this.listContainer = el)
            .children(purchases.stream().map(this::renderItem).toList()),
          div(Sel.PAGINATION).children(
            div(Sel.PAGE_PILL).children(
              div(Sel.PAGE_BTN)
                .on("click", useCallback("prev-" + page, mkOnPageChange(page - 1)))
                .children(span(Sel.PAGE_PREV_ICON)),
              span(Sel.PAGE_INFO).text(pageInfo),
              div(Sel.PAGE_BTN)
                .on("click", useCallback("next-" + page, mkOnPageChange(page + 1)))
                .children(span(Sel.PAGE_NEXT_ICON)))));
        // @formatter:on
    }

    private VNode renderItem(Purchase purchase) {
        // @formatter:off
        return div(Sel.ITEM_CARD).key(purchase.key())
          .on("click", useCallback("receipt-" + purchase.key(), mkOnOpenReceipt(purchase.id())))
          .children(
            div(Sel.ITEM_LINE1).children(
              span(Sel.ITEM_ID).text(purchase.idStr()),
              span(Sel.ITEM_DATE).text(purchase.date())),
            div(Sel.ITEM_LINE2).children(
              span(Sel.ITEM_ITEMS).text(purchase.items()),
              span(Sel.ITEM_TOTAL).text(purchase.total())));
        // @formatter:on
    }

    private void changePage(int newPage) {
        setFormField("p.page", newPage);
        submit(ON_PAGE_CHANGE);
    }

    private void scheduleResize() {
        if (pendingResizeFrame >= 0) {
            Window.cancelAnimationFrame(pendingResizeFrame);
        }
        pendingResizeFrame = Window.requestAnimationFrame(t -> {
            pendingResizeFrame = -1;
            computePageSize();
        });
    }

    private void computePageSize() {
        if (listContainer == null)
            return;
        int containerHeight = listContainer.getClientHeight();
        if (containerHeight <= 0) {
            Window.setTimeout(() -> Window.requestAnimationFrame(t -> computePageSize()), 200);
            return;
        }
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        setFormField("p.capacity", capacity);
        submit(ON_PAGE_SIZE_CHANGED);
    }

    @SuppressWarnings("deprecation")
    private static String formatDate(Object dateObj) {
        if (!(dateObj instanceof Number n))
            return "";
        long millis = n.longValue();
        if (millis <= 0)
            return "";
        var d = new Date(millis);
        int day = d.getDate();
        int month = d.getMonth() + 1;
        int year = d.getYear() + 1900;
        return (day < 10 ? "0" : "") + day + "/" + (month < 10 ? "0" : "") + month + "/" + year;
    }

    // :: State mapping helpers

    private List<Purchase> getPurchases(ViewScope scope) {
        if (scope == null)
            return List.of();
        var v = scope.getState().get("purchases");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Purchase>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m && m.containsKey("id")) {
                    result.add(getPurchase(m));
                }
            }
            return result;
        }
        return List.of();
    }

    private Purchase getPurchase(Map<?, ?> m) {
        var id = m.get("id");
        String idStr;
        String key;
        if (id instanceof Number n) {
            idStr = "#" + n.longValue();
            key = String.valueOf(n.longValue());
        } else if (id != null) {
            idStr = "#" + id;
            key = id.toString();
        } else {
            idStr = "";
            key = "";
        }
        var date = formatDate(m.get("date"));
        var itemsObj = m.get("items");
        String items;
        if (itemsObj instanceof List<?> il) {
            items = String.join(", ", il.stream().map(Object::toString).toList());
        } else {
            items = itemsObj != null ? itemsObj.toString() : "";
        }
        var totalVal = CoerceUtils.asNumber(m.get("total"));
        var total = totalVal != null ? "R$ " + String.format("%.2f", totalVal.doubleValue()) : "";
        return new Purchase(id, key, idStr, date, items, total);
    }

}
