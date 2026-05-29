package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import java.util.List;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class PurchasesPanelViewVDom extends AbstractVDomView<PurchasesPanelPresenter> {

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
        this.element.getClassList().add("p-2", "h-100", "d-flex", "flex-column");
        this.element.setAttribute("style", "text-align:left");
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

        return div("d-flex flex-column h-100").children(
                h6("fw-bold mb-1").text("Histórico de Compras"),
                p("text-muted small mb-0").text("Toque em uma compra para ver os detalhes"),

                // List container
                div("flex-grow-1")
                        .style("overflow:hidden;min-height:0")
                        .ref(el -> this.listContainer = el)
                        .children(purchases != null ? purchases.stream().map(this::renderItem).toList() : List.of()),

                // Pagination
                div("d-flex justify-content-center align-items-center gap-3 py-1 mt-auto").children(
                        button("btn btn-sm btn-outline-secondary")
                                .children(span(BsIcons.CHEVRON_LEFT))
                                .on("click", this.prevPageListener),
                        span("fw-bold text-muted").text(pageInfo),
                        button("btn btn-sm btn-outline-secondary")
                                .children(span(BsIcons.CHEVRON_RIGHT))
                                .on("click", this.nextPageListener)));
    }

    private VNode renderItem(PurchaseInfo purchase) {
        var id = "#" + purchase.id;
        var date = purchase.date > 0 ? DateUtils.formatDate(purchase.date) : "";
        var items = purchase.items != null ? String.join(", ", purchase.items) : "";
        var total = purchase.total > 0 ? "R$ " + String.format("%.2f", purchase.total) : "";

        return div("")
                .key(String.valueOf(purchase.id))
                .style(PURCHASE_ITEM)
                .on("click", evt -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(purchase.id)))
                .children(
                        // Line 1: #id (left) + date (right)
                        div("d-flex justify-content-between align-items-center")
                                .style("padding:6px 12px 0 12px")
                                .children(
                                        span("").style("font-weight:600;font-size:0.75rem;color:" + PRIMARY).text(id),
                                        span("").style("font-size:0.7rem;color:" + TEXT_MUTED).text(date)),
                        // Line 2: items (left) + total (right)
                        div("d-flex align-items-baseline")
                                .style("padding:2px 12px 6px 12px;gap:4px;font-size:0.75rem;color:" + TEXT_SECONDARY)
                                .children(
                                        span("")
                                                .style("flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap")
                                                .text(items),
                                        span("")
                                                .style("font-weight:bold;white-space:nowrap;color:" + TEXT_DARK)
                                                .text(total)));
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
