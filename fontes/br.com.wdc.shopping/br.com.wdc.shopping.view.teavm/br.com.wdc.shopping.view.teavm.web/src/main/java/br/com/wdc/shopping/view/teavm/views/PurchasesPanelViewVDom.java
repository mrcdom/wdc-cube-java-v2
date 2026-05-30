package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.VNode.*;

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
    private interface Styles {

        String ROOT = css()
                .flexCol()
                .flex("1")
                .minWidth("0")
                .minHeight("0")
                .overflowHidden()
                .padding("16px")
                .background("var(--app-surface)")
                .prop("border-left", "1px solid var(--app-border)")
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
                .prop("display", "block")
                .build();

        String LIST_CONTAINER = css()
                .flexGrow(1)
                .overflowHidden()
                .minHeight("0")
                .build();

        String PAGINATION = css()
                .displayFlex()
                .justifyContent("center")
                .alignItems("center")
                .padding("10px 0")
                .marginTop("auto")
                .prop("border-top", "1px solid var(--app-border)")
                .build();

        String PAGE_PILL = css()
                .prop("display", "inline-flex")
                .alignItems("center")
                .gap("4px")
                .background("var(--app-bg)")
                .borderRadius("20px")
                .padding("4px")
                .build();

        String PAGE_BTN = css()
                .width("28px")
                .height("28px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .borderRadius("50%")
                .cursor("pointer")
                .transition("background var(--app-transition)")
                .build();

        String PAGE_BTN_ICON = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String PAGE_INFO = css()
                .padding("4px 12px")
                .fontWeight("600")
                .fontSize("0.75rem")
                .color("var(--app-text)")
                .background("var(--app-surface)")
                .borderRadius("12px")
                .boxShadow("var(--app-shadow-sm)")
                .build();

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

        String ITEM_LINE1 = css()
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

        String ITEM_LINE2 = css()
                .displayFlex()
                .alignItems("baseline")
                .padding("4px 12px 8px 12px")
                .gap("4px")
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .minWidth("0")
                .build();

        String ITEM_ITEMS = css()
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

    private static final int ITEM_HEIGHT_PX = 56;
    private static final int MAX_COMPUTE_RETRIES = 10;

    private final PurchasesPanelViewState state;
    private HTMLElement listContainer;
    private int pendingResizeFrame = -1;
    private int computeRetries;

    // Stable event listener references (avoid re-registration on every render)
    private final EventListener<Event> prevPageListener;
    private final EventListener<Event> nextPageListener;

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
        var purchases = this.state.purchases;
        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
        var pageInfo = (this.state.page + 1) + " / " + totalPages;

        // @formatter:off
        return div().style(Styles.ROOT).children(
          // Header
          div().style(Styles.HEADER_ROW).children(
            span("bi bi-clock-history").style(Styles.HEADER_ICON),
            span().style(Styles.HEADER_TITLE).text("Histórico")),
          span().style(Styles.HINT).text("Toque para ver detalhes"),
          // List container
          div().style(Styles.LIST_CONTAINER)
            .ref(el -> this.listContainer = el)
            .children(purchases != null ? purchases.stream().map(this::renderItem).toList() : List.of()),
          // Pagination
          div().style(Styles.PAGINATION).children(
            div().style(Styles.PAGE_PILL).children(
              div().style(Styles.PAGE_BTN)
                .on("click", this.prevPageListener)
                .children(span("bi bi-chevron-left").style(Styles.PAGE_BTN_ICON)),
              span().style(Styles.PAGE_INFO).text(pageInfo),
              div().style(Styles.PAGE_BTN)
                .on("click", this.nextPageListener)
                .children(span("bi bi-chevron-right").style(Styles.PAGE_BTN_ICON)))));
        // @formatter:on
    }

    private VNode renderItem(PurchaseInfo purchase) {
        var id = "#" + purchase.id;
        var date = purchase.date > 0 ? DateUtils.formatDate(purchase.date) : "";
        var items = purchase.items != null ? String.join(", ", purchase.items) : "";
        var total = purchase.total > 0 ? "R$ " + String.format("%.2f", purchase.total) : "";

        // @formatter:off
        return div("purchase-item").key(String.valueOf(purchase.id)).style(Styles.ITEM_CARD)
          .on("click", evt -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(purchase.id)))
          .children(
            div().style(Styles.ITEM_LINE1).children(
              span().style(Styles.ITEM_ID).text(id),
              span().style(Styles.ITEM_DATE).text(date)),
            div().style(Styles.ITEM_LINE2).children(
              span().style(Styles.ITEM_ITEMS).text(items),
              span().style(Styles.ITEM_TOTAL).text(total)));
        // @formatter:on
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
