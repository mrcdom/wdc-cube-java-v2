package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.VNode.*;

import br.com.wdc.framework.vdom.CssIcons;
import java.util.List;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class PurchasesPanelViewVDom extends AbstractVDomView<PurchasesPanelPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = "purchases-panel";
        String HEADER_ROW = "purchases-header-row";
        String HEADER_ICON = clsx(CssIcons.CLOCK_HISTORY, "purchases-header-icon");
        String HEADER_TITLE = "purchases-header-title";
        String HINT = "purchases-hint";
        String LIST_CONTAINER = "purchases-list-container";
        String PAGINATION = "purchases-pagination";
        String PAGE_PILL = "purchases-page-pill";
        String PAGE_BTN = "purchases-page-btn";
        String PAGE_PREV_ICON = clsx(CssIcons.CHEVRON_LEFT, "purchases-page-btn-icon");
        String PAGE_NEXT_ICON = clsx(CssIcons.CHEVRON_RIGHT, "purchases-page-btn-icon");
        String PAGE_INFO = "purchases-page-info";
        String ITEM_CARD = clsx("purchase-item", "purchases-item-card");
        String ITEM_LINE1 = "purchases-item-line1";
        String ITEM_ID = "purchases-item-id";
        String ITEM_DATE = "purchases-item-date";
        String ITEM_LINE2 = "purchases-item-line2";
        String ITEM_ITEMS = "purchases-item-items";
        String ITEM_TOTAL = "purchases-item-total";
    }

    private static final int ITEM_HEIGHT_PX = 56;
    private static final int MAX_COMPUTE_RETRIES = 10;

    private final PurchasesPanelViewState state;
    private HTMLElement listContainer;
    private int pendingResizeFrame = -1;
    private int computeRetries;

    // Stable event listener references (avoid re-registration on every render)
    private final EventListener<Event> prevPageListener;
    private final EventListener<Event> nextPageListener;

    private record PurchaseData(long id, String key, String idStr, String date, String items, String total) {}

    public PurchasesPanelViewVDom(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
        this.prevPageListener = evt -> safeAction("Prev page",
                () -> this.presenter.onPageChange(this.state.page - 1));
        this.nextPageListener = evt -> safeAction("Next page",
                () -> this.presenter.onPageChange(this.state.page + 1));
        Window.current().addEventListener("resize", evt -> scheduleResize());
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        if (this.state.pageSize < 0) {
            Window.requestAnimationFrame(t -> computePageSize());
        }
    }

    @Override
    protected VNode render() {
        var purchases = getPurchases();
        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
        var pageInfo = (this.state.page + 1) + " / " + totalPages;

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.HEADER_ROW).children(
            span(Css.HEADER_ICON),
            span(Css.HEADER_TITLE).text("Histórico")),
          span(Css.HINT).text("Toque para ver detalhes"),
          div(Css.LIST_CONTAINER)
            .ref(el -> this.listContainer = el)
            .children(purchases.stream().map(this::renderItem).toList()),
          div(Css.PAGINATION).children(
            div(Css.PAGE_PILL).children(
              div(Css.PAGE_BTN)
                .on("click", this.prevPageListener)
                .children(span(Css.PAGE_PREV_ICON)),
              span(Css.PAGE_INFO).text(pageInfo),
              div(Css.PAGE_BTN)
                .on("click", this.nextPageListener)
                .children(span(Css.PAGE_NEXT_ICON)))));
        // @formatter:on
    }

    private VNode renderItem(PurchaseData purchase) {
        // @formatter:off
        return div(Css.ITEM_CARD).key(purchase.key())
          .on("click", evt -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(purchase.id())))
          .children(
            div(Css.ITEM_LINE1).children(
              span(Css.ITEM_ID).text(purchase.idStr()),
              span(Css.ITEM_DATE).text(purchase.date())),
            div(Css.ITEM_LINE2).children(
              span(Css.ITEM_ITEMS).text(purchase.items()),
              span(Css.ITEM_TOTAL).text(purchase.total())));
        // @formatter:on
    }

    private List<PurchaseData> getPurchases() {
        List<PurchaseInfo> list = this.state.purchases;
        if (list == null || list.isEmpty()) return List.of();
        return list.stream().map(p -> {
            var key = String.valueOf(p.id);
            var idStr = "#" + p.id;
            var date = p.date > 0 ? DateUtils.formatDate(p.date) : "";
            var items = p.items != null ? String.join(", ", p.items) : "";
            var total = p.total > 0 ? "R$ " + String.format("%.2f", p.total) : "";
            return new PurchaseData(p.id, key, idStr, date, items, total);
        }).toList();
    }

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
        if (this.listContainer == null)
            return;
        int containerHeight = this.listContainer.getClientHeight();
        if (containerHeight <= 0) {
            // Element not yet laid out (e.g. not attached to DOM); retry next frame
            if (this.computeRetries < MAX_COMPUTE_RETRIES) {
                this.computeRetries++;
                Window.requestAnimationFrame(t -> computePageSize());
            }
            return;
        }
        this.computeRetries = 0;
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        // Must run in Thread because onItemSizeCapacityChanged triggers @Async data fetch
        safeAction("PageSize", () -> this.presenter.onItemSizeCapacityChanged(capacity));
    }
}
