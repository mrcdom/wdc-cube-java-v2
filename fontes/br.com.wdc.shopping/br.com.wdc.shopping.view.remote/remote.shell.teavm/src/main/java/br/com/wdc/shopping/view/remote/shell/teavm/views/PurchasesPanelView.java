package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.vdom.VNode;

/**
 * Purchases history panel with pagination.
 * State: purchases (array of {id, date, items, total}), page, totalCount, pageSize.
 */
public class PurchasesPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "b3c4d5e6f7a8";

    private static final int ON_OPEN_RECEIPT = 1;
    private static final int ON_PAGE_CHANGE = 2;
    private static final int ON_PAGE_SIZE_CHANGED = 3;
    private static final int ITEM_HEIGHT_PX = 56;

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String ROOT = css()
                .flexCol()
                .flex("1")
                .minWidth("0")
                .minHeight("0")
                .overflowHidden()
                .padding("16px")
                .background("var(--app-surface)")
                .borderLeft("1px solid var(--app-border)")
                .build();

        String HEADER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("8px")
                .marginBottom("12px")
                .build();

        String HEADER_ICON = css()
                .fontSize("1rem")
                .color("var(--app-accent)")
                .build();

        String HEADER_TITLE = css()
                .fontWeight("700")
                .fontSize("0.9rem")
                .color("var(--app-text)")
                .build();

        String HINT = css()
                .color("var(--app-text-secondary)")
                .fontSize("0.75rem")
                .marginBottom("12px")
                .displayBlock()
                .build();

        String LIST_CONTAINER = css()
                .flexGrow(1)
                .overflowHidden()
                .minHeight("0")
                .build();

        String PAGER_WRAP = css()
                .displayFlex()
                .justifyContent("center")
                .alignItems("center")
                .padding("10px 0")
                .marginTop("auto")
                .borderTop("1px solid var(--app-border)")
                .build();

        String PAGER_GROUP = css()
                .displayInlineFlex()
                .alignItems("center")
                .gap("4px")
                .background("var(--app-bg)")
                .borderRadius("20px")
                .padding("4px")
                .build();

        String PAGER_BTN = css()
                .width("28px")
                .height("28px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .borderRadius("50%")
                .cursor("pointer")
                .transition("background var(--app-transition)")
                .build();

        String PAGER_ICON = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String PAGER_INFO = css()
                .padding("4px 12px")
                .fontWeight("600")
                .fontSize("0.75rem")
                .color("var(--app-text)")
                .background("var(--app-surface)")
                .borderRadius("12px")
                .boxShadow("var(--app-shadow-sm)")
                .build();

        // Purchase item
        String ITEM_CARD = css()
                .width("100%")
                .boxSizing("border-box")
                .background("var(--app-bg)")
                .borderRadius("var(--app-radius-sm)")
                .cursor("pointer")
                .marginBottom("8px")
                .overflowHidden()
                .border("1px solid var(--app-border)")
                .build();

        String ITEM_TOP_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .alignItems("center")
                .padding("8px 12px 0 12px")
                .minWidth("0")
                .build();

        String ITEM_ID = css()
                .fontWeight("600")
                .fontSize("0.75rem")
                .color("var(--app-accent)")
                .build();

        String ITEM_DATE = css()
                .fontSize("0.7rem")
                .color("var(--app-text-secondary)")
                .whiteSpace("nowrap")
                .build();

        String ITEM_BOTTOM_ROW = css()
                .displayFlex()
                .alignItems("baseline")
                .padding("4px 12px 8px 12px")
                .gap("4px")
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .minWidth("0")
                .build();

        String ITEM_DESCRIPTION = css()
                .flex("1")
                .minWidth("0")
                .overflowHidden()
                .textOverflow("ellipsis")
                .whiteSpace("nowrap")
                .build();

        String ITEM_TOTAL = css()
                .fontWeight("700")
                .whiteSpace("nowrap")
                .color("var(--app-text)")
                .build();
    }

    private HTMLElement listContainer;
    private int pendingResizeFrame = -1;

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
        List<Map<String, Object>> purchases = getPurchases();
        int page = scope.getInt("page");
        int totalCount = scope.getInt("totalCount");
        int pageSize = Math.max(1, scope.getInt("pageSize"));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
        var pageInfo = (page + 1) + " / " + totalPages;

        // @formatter:off
        return div().style(Styles.ROOT).children(
          div().style(Styles.HEADER_ROW).children(
            span("bi bi-clock-history").style(Styles.HEADER_ICON),
            span().style(Styles.HEADER_TITLE).text("Histórico")),
          span().style(Styles.HINT).text("Toque para ver detalhes"),
          div().style(Styles.LIST_CONTAINER)
            .ref(el -> this.listContainer = el)
            .children(purchases.stream().map(this::renderItem).toList()),
          div().style(Styles.PAGER_WRAP).children(
            div().style(Styles.PAGER_GROUP).children(
              div().style(Styles.PAGER_BTN)
                .on("click", evt -> changePage(page - 1))
                .children(span("bi bi-chevron-left").style(Styles.PAGER_ICON)),
              span().style(Styles.PAGER_INFO).text(pageInfo),
              div().style(Styles.PAGER_BTN)
                .on("click", evt -> changePage(page + 1))
                .children(span("bi bi-chevron-right").style(Styles.PAGER_ICON)))));
        // @formatter:on
    }

    private VNode renderItem(Map<String, Object> purchase) {
        var id = purchase.get("id");
        String idStr;
        if (id instanceof Number n) {
            idStr = "#" + n.intValue();
        } else {
            idStr = id != null ? "#" + id : "";
        }
        var date = formatDate(purchase.get("date"));
        var items = purchase.get("items");
        String itemsStr;
        if (items instanceof List<?> list) {
            itemsStr = String.join(", ", list.stream().map(Object::toString).toList());
        } else {
            itemsStr = items != null ? items.toString() : "";
        }
        var totalVal = purchase.get("total");
        var total = totalVal instanceof Number n ? "R$ " + String.format("%.2f", n.doubleValue()) : "";
        String key;
        if (id instanceof Number n) {
            key = String.valueOf(n.intValue());
        } else {
            key = id != null ? id.toString() : date;
        }

        // @formatter:off
        return div("purchase-item").key(key).style(Styles.ITEM_CARD)
          .on("click", evt -> { setFormField("p.purchaseId", id); submit(ON_OPEN_RECEIPT); })
          .children(
            div().style(Styles.ITEM_TOP_ROW).children(
              span().style(Styles.ITEM_ID).text(idStr),
              span().style(Styles.ITEM_DATE).text(date)),
            div().style(Styles.ITEM_BOTTOM_ROW).children(
              span().style(Styles.ITEM_DESCRIPTION).text(itemsStr),
              span().style(Styles.ITEM_TOTAL).text(total)));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPurchases() {
        var scope = state();
        if (scope == null) return List.of();
        var v = scope.getState().get("purchases");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Map<String, Object>>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
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
        if (listContainer == null) return;
        int containerHeight = listContainer.getClientHeight();
        if (containerHeight <= 0) {
            // Container not visible yet (e.g. hidden tab on mobile), retry
            Window.setTimeout(() -> Window.requestAnimationFrame(t -> computePageSize()), 200);
            return;
        }
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        setFormField("p.capacity", capacity);
        submit(ON_PAGE_SIZE_CHANGED);
    }

    @SuppressWarnings("deprecation")
    private static String formatDate(Object dateObj) {
        if (!(dateObj instanceof Number n)) return "";
        long millis = n.longValue();
        if (millis <= 0) return "";
        var d = new Date(millis);
        int day = d.getDate();
        int month = d.getMonth() + 1;
        int year = d.getYear() + 1900;
        return (day < 10 ? "0" : "") + day + "/" + (month < 10 ? "0" : "") + month + "/" + year;
    }
}
