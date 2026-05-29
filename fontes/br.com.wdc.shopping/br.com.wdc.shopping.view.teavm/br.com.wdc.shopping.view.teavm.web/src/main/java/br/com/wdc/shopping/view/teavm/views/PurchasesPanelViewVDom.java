package br.com.wdc.shopping.view.teavm.views;

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
        return div("").style("display:flex;flex-direction:column;flex:1;min-width:0;min-height:0;overflow:hidden;padding:16px;background:var(--app-surface);border-left:1px solid var(--app-border)").children(
                // Header
                div("").style("display:flex;align-items:center;gap:8px;margin-bottom:12px").children(
                        span("bi bi-clock-history").style("font-size:1rem;color:var(--app-accent)"),
                        span("").style("font-weight:700;font-size:0.9rem;color:var(--app-text)").text("Histórico")),
                span("").style("color:var(--app-text-secondary);font-size:0.75rem;margin-bottom:12px;display:block").text("Toque para ver detalhes"),

                // List container
                div("")
                        .style("flex-grow:1;overflow:hidden;min-height:0")
                        .ref(el -> this.listContainer = el)
                        .children(purchases != null ? purchases.stream().map(this::renderItem).toList() : List.of()),

                // Pagination
                div("").style("display:flex;justify-content:center;align-items:center;padding:10px 0;margin-top:auto;border-top:1px solid var(--app-border)").children(
                        div("")
                                .style("display:inline-flex;align-items:center;gap:4px;background:var(--app-bg);border-radius:20px;padding:4px")
                                .children(
                                        div("")
                                                .style("width:28px;height:28px;display:flex;align-items:center;justify-content:center;border-radius:50%;cursor:pointer;transition:background var(--app-transition)")
                                                .on("click", this.prevPageListener)
                                                .children(span("bi bi-chevron-left").style("font-size:0.75rem;color:var(--app-text-secondary)")),
                                        span("")
                                                .style("padding:4px 12px;font-weight:600;font-size:0.75rem;color:var(--app-text);background:var(--app-surface);border-radius:12px;box-shadow:var(--app-shadow-sm)")
                                                .text(pageInfo),
                                        div("")
                                                .style("width:28px;height:28px;display:flex;align-items:center;justify-content:center;border-radius:50%;cursor:pointer;transition:background var(--app-transition)")
                                                .on("click", this.nextPageListener)
                                                .children(span("bi bi-chevron-right").style("font-size:0.75rem;color:var(--app-text-secondary)")))));
        // @formatter:on
    }

    private VNode renderItem(PurchaseInfo purchase) {
        var id = "#" + purchase.id;
        var date = purchase.date > 0 ? DateUtils.formatDate(purchase.date) : "";
        var items = purchase.items != null ? String.join(", ", purchase.items) : "";
        var total = purchase.total > 0 ? "R$ " + String.format("%.2f", purchase.total) : "";

        // @formatter:off
        return div("purchase-item")
                .key(String.valueOf(purchase.id))
                .style("width:100%;box-sizing:border-box;background:var(--app-bg);border-radius:var(--app-radius-sm);cursor:pointer;margin-bottom:8px;overflow:hidden;border:1px solid var(--app-border)")
                .on("click", evt -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(purchase.id)))
                .children(
                        // Line 1: #id (left) + date (right)
                        div("")
                                .style("display:flex;justify-content:space-between;align-items:center;padding:8px 12px 0 12px;min-width:0")
                                .children(
                                        span("").style("font-weight:600;font-size:0.75rem;color:var(--app-accent)").text(id),
                                        span("").style("font-size:0.7rem;color:var(--app-text-secondary);white-space:nowrap").text(date)),
                        // Line 2: items (left) + total (right)
                        div("")
                                .style("display:flex;align-items:baseline;padding:4px 12px 8px 12px;gap:4px;font-size:0.75rem;color:var(--app-text-secondary);min-width:0")
                                .children(
                                        span("")
                                                .style("flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap")
                                                .text(items),
                                        span("")
                                                .style("font-weight:700;white-space:nowrap;color:var(--app-text)")
                                                .text(total)));
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
