package br.com.wdc.shopping.view.teavm.commons.views.home;

import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.teavm.commons.DateUtils;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared Purchases history panel with pagination and auto page-size.
 */
public class PurchasesPanelSharedView extends SharedVDomView {

    private static final int ITEM_HEIGHT_PX = 56;
    private static final int MAX_COMPUTE_RETRIES = 10;

    @SuppressWarnings({"java:S1214", "static-access"})
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

    // -- External bindings --

    public Supplier<PurchasesPanelViewState> stateSupplier;
    public Consumer<Long> onOpenReceipt;
    public Consumer<Integer> onPageChange;
    public Consumer<Integer> onPageSizeChanged;
    public Runnable requestUpdate;
    /** Optional custom date formatter. Falls back to DateUtils.formatDate. */
    public LongFunction<String> dateFormatter;

    // -- Local state --

    private HTMLElement listContainer;
    private int pendingResizeFrame = -1;
    private int computeRetries;

    public PurchasesPanelSharedView() {
        Window.current().addEventListener("resize", evt -> scheduleResize());
    }

    // -- Lifecycle --

    /**
     * Called after DOM patch. Checks if page size needs computing.
     */
    public void afterUpdate() {
        var state = stateSupplier.get();
        if (state.pageSize <= 0) {
            Window.requestAnimationFrame(t -> computePageSize());
        }
    }

    // -- Render --

    private EventListener<Event> mkOnOpenReceipt(long id) {
        return evt -> onOpenReceipt.accept(id);
    }

    private EventListener<Event> mkOnPageChange(int page) {
        return evt -> onPageChange.accept(page);
    }

    @Override
    public VNode render() {
        var state = stateSupplier.get();
        var purchases = state.purchases;
        int page = state.page;
        int pageSize = Math.max(1, state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) state.totalCount / pageSize));
        var pageInfo = (page + 1) + " / " + totalPages;

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.HEADER_ROW).children(
            span(Sel.HEADER_ICON),
            span(Sel.HEADER_TITLE).text("Histórico")),
          span(Sel.HINT).text("Toque para ver detalhes"),
          div(Sel.LIST_CONTAINER)
            .ref(el -> this.listContainer = el)
            .children(purchases != null ? purchases.stream().map(this::renderItem).toList() : List.of()),
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

    private VNode renderItem(PurchaseInfo purchase) {
        var key = String.valueOf(purchase.id);
        var idStr = "#" + purchase.id;
        var date = purchase.date > 0 ? formatDate(purchase.date) : "";
        var items = purchase.items != null ? String.join(", ", purchase.items) : "";
        var total = purchase.total > 0 ? "R$ " + String.format("%.2f", purchase.total) : "";

        // @formatter:off
        return div(Sel.ITEM_CARD).key(key)
          .on("click", useCallback("receipt-" + key, mkOnOpenReceipt(purchase.id)))
          .children(
            div(Sel.ITEM_LINE1).children(
              span(Sel.ITEM_ID).text(idStr),
              span(Sel.ITEM_DATE).text(date)),
            div(Sel.ITEM_LINE2).children(
              span(Sel.ITEM_ITEMS).text(items),
              span(Sel.ITEM_TOTAL).text(total)));
        // @formatter:on
    }

    private String formatDate(long millis) {
        if (dateFormatter != null) return dateFormatter.apply(millis);
        return DateUtils.formatDate(millis);
    }

    // -- Resize logic --

    private void scheduleResize() {
        if (this.pendingResizeFrame >= 0) {
            Window.cancelAnimationFrame(this.pendingResizeFrame);
        }
        this.pendingResizeFrame = Window.requestAnimationFrame(t -> {
            this.pendingResizeFrame = -1;
            computePageSize();
        });
    }

    private void computePageSize() {
        if (this.listContainer == null) return;
        int containerHeight = this.listContainer.getClientHeight();
        if (containerHeight <= 0) {
            if (this.computeRetries < MAX_COMPUTE_RETRIES) {
                this.computeRetries++;
                Window.requestAnimationFrame(t -> computePageSize());
            }
            return;
        }
        this.computeRetries = 0;
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        onPageSizeChanged.accept(capacity);
    }
}
