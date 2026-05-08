package br.com.wdc.shopping.view.teavm.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class PurchasesPanelViewTeaVM extends AbstractViewTeaVM<PurchasesPanelPresenter> {

    private static final int ITEM_HEIGHT_PX = 56;

    private final PurchasesPanelViewState state;

    private int itemIdx;
    private List<PurchaseItemView> viewList = new ArrayList<>();
    private BiConsumer<List<PurchaseInfo>, List<PurchaseItemView>> contentSlot;
    private HTMLElement listContainer;
    private HTMLElement pageInfoElm;
    private int pendingResizeFrame = -1;

    public PurchasesPanelViewTeaVM(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
        this.element.getClassList().add("p-2", "h-100", "d-flex", "flex-column");
        Window.current().addEventListener("resize", evt -> scheduleResize());
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }
        if (this.state.pageSize < 0) {
            Window.requestAnimationFrame(t -> computePageSize());
        }
        this.contentSlot.accept(this.state.purchases, this.viewList);

        int pageSize = Math.max(1, this.state.pageSize);
        int totalPages = Math.max(1, (int) Math.ceil((double) this.state.totalCount / pageSize));
        this.pageInfoElm.setTextContent((this.state.page + 1) + " / " + totalPages);
    }

    private void buildUI(HtmlDom dom, HTMLElement root) {
        dom.h6("fw-bold mb-1", caption -> {
            caption.setTextContent("Histórico de Compras");
        });
        dom.p("text-muted small mb-0", subtitle -> {
            subtitle.setTextContent("Toque em uma compra para ver os detalhes");
        });

        this.listContainer = dom.div("flex-grow-1", container -> {
            container.setAttribute("style", "overflow-y:auto;overflow-x:hidden");
        });
        this.contentSlot = this.newListSlot(this.listContainer, this::newItemView, this::updateItem);

        // Pagination
        dom.div("d-flex justify-content-center align-items-center gap-3 py-1 mt-auto", pagination -> {
            dom.button("btn btn-sm btn-outline-secondary", prevBtn -> {
                dom.icon(BsIcons.CHEVRON_LEFT);
                prevBtn.addEventListener("click",
                        evt -> safeAction("Prev page", () -> this.presenter.onPageChange(this.state.page - 1)));
            });

            this.pageInfoElm = dom.span("fw-bold text-muted", pageInfo -> {
                pageInfo.setTextContent("1 / 1");
            });

            dom.button("btn btn-sm btn-outline-secondary", nextBtn -> {
                dom.icon(BsIcons.CHEVRON_RIGHT);
                nextBtn.addEventListener("click",
                        evt -> safeAction("Next page", () -> this.presenter.onPageChange(this.state.page + 1)));
            });
        });
    }

    private PurchaseItemView newItemView() {
        return new PurchaseItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(PurchaseItemView itemView, PurchaseInfo state) {
        itemView.setState(state);
        itemView.doUpdate();
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
        if (this.listContainer == null) return;
        // Restore flex-grow-1 class for accurate measurement
        this.listContainer.getClassList().add("flex-grow-1");
        int containerHeight = this.listContainer.getClientHeight();
        if (containerHeight <= 0) return;
        int capacity = Math.max(1, containerHeight / ITEM_HEIGHT_PX);
        // Remove flex-grow so list shrinks to content; paginator sits right below
        this.listContainer.getClassList().remove("flex-grow-1");
        this.presenter.onItemSizeCapacityChanged(capacity);
    }

    // ---- Inner class ----

    public static class PurchaseItemView extends AbstractViewTeaVM<PurchasesPanelPresenter> {

        private PurchaseInfo purchase;
        private HTMLElement idElm;
        private HTMLElement dateElm;
        private HTMLElement itemsElm;
        private HTMLElement totalElm;
        private long idOldValue;
        private String dateOldValue;
        private String itemsOldValue;
        private String totalOldValue;

        PurchaseItemView(ShoppingTeaVMApplication app, PurchasesPanelPresenter presenter, int idx) {
            super("purchase-item-" + idx, app, presenter,
                    HTMLDocument.current().createElement("div"));
            this.purchase = new PurchaseInfo();
            this.element.setAttribute("style",
                    "background-color:#fafafa;border-radius:6px;border-left:3px solid #1976d2;"
                    + "cursor:pointer;transition:all 0.15s;margin-bottom:6px;overflow:hidden");
            this.element.addEventListener("mouseenter", evt -> {
                this.element.setAttribute("style",
                        "background-color:#e3f2fd;border-radius:6px;border-left:3px solid #1976d2;"
                        + "cursor:pointer;transition:all 0.15s;margin-bottom:6px;overflow:hidden;transform:translateX(2px)");
            });
            this.element.addEventListener("mouseleave", evt -> {
                this.element.setAttribute("style",
                        "background-color:#fafafa;border-radius:6px;border-left:3px solid #1976d2;"
                        + "cursor:pointer;transition:all 0.15s;margin-bottom:6px;overflow:hidden");
            });
        }

        void setState(PurchaseInfo purchase) {
            this.purchase = purchase;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                HtmlDom.render(this.element, this::buildUI);
                this.notRendered = false;
            }

            if (this.idOldValue != this.purchase.id) {
                this.idElm.setTextContent("#" + this.purchase.id);
                this.idOldValue = this.purchase.id;
            }

            var dateNewValue = this.purchase.date > 0
                    ? DateUtils.formatDate(this.purchase.date)
                    : "";
            if (!Objects.equals(this.dateOldValue, dateNewValue)) {
                this.dateElm.setTextContent(dateNewValue);
                this.dateOldValue = dateNewValue;
            }

            var itemsNewValue = formatItems(this.purchase.items);
            if (!Objects.equals(this.itemsOldValue, itemsNewValue)) {
                this.itemsElm.setTextContent(itemsNewValue);
                this.itemsOldValue = itemsNewValue;
            }

            var totalNewValue = this.purchase.total > 0
                    ? "R$ " + String.format("%.2f", this.purchase.total)
                    : "";
            if (!Objects.equals(this.totalOldValue, totalNewValue)) {
                this.totalElm.setTextContent(totalNewValue);
                this.totalOldValue = totalNewValue;
            }
        }

        private void buildUI(HtmlDom dom, HTMLElement card) {
            card.addEventListener("click",
                    evt -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(this.purchase.id)));

            // Line 1: #id (left) + date (right)
            dom.div("d-flex justify-content-between align-items-center", line1 -> {
                line1.setAttribute("style", "padding:6px 12px 0 12px");
                this.idElm = dom.span("", id -> {
                    id.setAttribute("style", "font-weight:600;font-size:0.75rem;color:#1976d2");
                });
                this.dateElm = dom.span("", date -> {
                    date.setAttribute("style", "font-size:0.7rem;color:#888");
                });
            });

            // Line 2: items (left) + total (right)
            dom.div("d-flex align-items-baseline", line2 -> {
                line2.setAttribute("style", "padding:2px 12px 6px 12px;gap:4px;font-size:0.75rem;color:#666");
                this.itemsElm = dom.span("", items -> {
                    items.setAttribute("style", "flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap");
                });
                this.totalElm = dom.span("", total -> {
                    total.setAttribute("style", "font-weight:bold;white-space:nowrap;color:#333");
                });
            });
        }

        private static String formatItems(java.util.List<String> items) {
            if (items == null || items.isEmpty()) {
                return "";
            }
            if (items.size() == 1) {
                return items.get(0);
            }
            return items.get(0) + ", +" + (items.size() - 1) + "...";
        }
    }
}
