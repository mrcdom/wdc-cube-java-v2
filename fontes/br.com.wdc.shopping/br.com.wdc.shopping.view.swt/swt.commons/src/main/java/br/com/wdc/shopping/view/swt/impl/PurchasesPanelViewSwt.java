package br.com.wdc.shopping.view.swt.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

/**
 * Purchases panel — list of purchase items with pagination.
 */
public class PurchasesPanelViewSwt extends AbstractViewSwt {

    public Supplier<PurchasesPanelViewState> stateSupplier;
    public IntConsumer onPageChange;
    public IntConsumer onItemSizeCapacityChanged;
    public LongConsumer onOpenReceipt;

    private static final int ITEM_HEIGHT = 50;
    private static final int LIST_SPACING = 8;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private boolean notRendered = true;
    private Composite listPanel;
    private final List<PurchaseItemSlot> itemSlots = new ArrayList<>();
    private Canvas paginationCanvas;
    private int lastHash;
    private int lastCapacity;

    public PurchasesPanelViewSwt(SwtApp app) {
        super("purchases-panel", app);
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        // Rebuild list if data changed
        var state = this.stateSupplier != null ? this.stateSupplier.get() : null;
        var purchases = state != null ? state.purchases : null;
        int hash = purchases != null ? purchases.hashCode() : 0;
        hash = hash * 31 + (state != null ? state.page : 0);
        hash = hash * 31 + (state != null ? state.totalCount : 0);
        if (hash != this.lastHash) {
            this.lastHash = hash;
            rebuildList(purchases);
            updatePagination();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.listPanel = null;
        this.itemSlots.clear();
        this.paginationCanvas = null;
        this.lastHash = 0;
        this.lastCapacity = 0;
    }

    private void initialRender() {
        var layout = new GridLayout(1, false);
        layout.marginWidth = 12;
        layout.marginHeight = 12;
        layout.verticalSpacing = 4;
        this.element.setLayout(layout);
        this.element.setBackground(Theme.BG_WHITE);

        SwtDom.render(this.element, (dom, root) -> {
            // Header with icon + title
            dom.row(2, headerRow -> {
                headerRow.setBackground(Theme.BG_WHITE);
                var gd = new GridData();
                gdFillH(gd);
                gdTop(gd);
                headerRow.setLayoutData(gd);
                var hrLayout = (GridLayout) headerRow.getLayout();
                hrLayout.marginHeight = 4;
                hrLayout.horizontalSpacing = 8;

                dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_ICON);
                    lbl.setText(Theme.ICON_CLOCK_HISTORY);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
                });

                dom.label(lbl -> {
                    lbl.setFont(Theme.FONT_HEADER);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setText("Histórico");
                    lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
                });
            });

            // Subtitle
            dom.label(lbl -> {
                lbl.setFont(Theme.FONT_BODY);
                lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                lbl.setBackground(Theme.BG_WHITE);
                lbl.setText("Toque para ver detalhes");
                var gd = new GridData();
                gdFillH(gd);
                gdTop(gd);
                lbl.setLayoutData(gd);
            });

            // List area
            this.listPanel = dom.col(panel -> {
                panel.setBackground(Theme.BG_WHITE);
                panel.setLayoutData(gdFill(new GridData()));
                var listLayout = (GridLayout) panel.getLayout();
                listLayout.verticalSpacing = LIST_SPACING;
            });

            // Recompute capacity on every resize (including first layout)
            this.listPanel.addListener(SWT.Resize, _e -> this.checkCapacityChanged());

            // Pagination footer
            this.paginationCanvas = dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                canvas.setBackground(Theme.BG_WHITE);
                var paginationGd = new GridData();
                gdCenter(paginationGd);
                gdGrabH(paginationGd);
                gdBottom(paginationGd);
                paginationGd.widthHint = 120;
                paginationGd.heightHint = 36;
                canvas.setLayoutData(paginationGd);
                canvas.setCursor(canvas.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
                canvas.addPaintListener(ev -> paintPagination(ev.gc, canvas.getClientArea()));
                canvas.addListener(SWT.MouseDown, ev -> {
                    var area = canvas.getClientArea();
                    int arrowSize = 28;
                    if (ev.x < arrowSize + 4) {
                        var st = PurchasesPanelViewSwt.this.stateSupplier != null ? PurchasesPanelViewSwt.this.stateSupplier.get() : null;
                        int page = st != null ? st.page : 0;
                        if (page > 0) {
                            safeAction("prevPage", () -> { if (PurchasesPanelViewSwt.this.onPageChange != null) PurchasesPanelViewSwt.this.onPageChange.accept(page - 1); });
                        }
                    } else if (ev.x > area.width - arrowSize - 4) {
                        int totalPages = getTotalPages();
                        var st2 = PurchasesPanelViewSwt.this.stateSupplier != null ? PurchasesPanelViewSwt.this.stateSupplier.get() : null;
                        int page = st2 != null ? st2.page : 0;
                        if (page < totalPages - 1) {
                            safeAction("nextPage", () -> { if (PurchasesPanelViewSwt.this.onPageChange != null) PurchasesPanelViewSwt.this.onPageChange.accept(page + 1); });
                        }
                    }
                });
            });
        });
    }

    private void rebuildList(List<PurchaseInfo> purchases) {
        if (this.listPanel == null || this.listPanel.isDisposed()) return;

        var items = purchases != null ? purchases : Collections.<PurchaseInfo>emptyList();
        syncList(this.listPanel, items, this.itemSlots,
                () -> new PurchaseItemSlot(this.listPanel),
                (slot, purchase) -> {
                    slot.item = purchase;
                    slot.redraw();
                });
    }
    
    private void checkCapacityChanged() {
    	int h = this.listPanel.getClientArea().height;
        if (h > 0) {
            int capacity = Math.max(1, (h + LIST_SPACING) / (ITEM_HEIGHT + LIST_SPACING));
            if (capacity != this.lastCapacity) {
                this.lastCapacity = capacity;
                safeAction("capacityChanged", () -> { if (PurchasesPanelViewSwt.this.onItemSizeCapacityChanged != null) PurchasesPanelViewSwt.this.onItemSizeCapacityChanged.accept(capacity); });
            }
        }
    }

    // ========== PURCHASE ITEM SLOT ==========

    private class PurchaseItemSlot extends Canvas {

        PurchaseInfo item;

        PurchaseItemSlot(Composite parent) {
            super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
            var gd = new GridData(SWT.FILL, SWT.TOP, true, false);
            gd.heightHint = ITEM_HEIGHT;
            setLayoutData(gd);
            setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));

            addPaintListener(e -> paint(e.gc, getClientArea()));
            addListener(SWT.MouseDown, _e -> {
                var current = this.item;
                if (current != null) {
                    safeAction("openReceipt", () -> { if (PurchasesPanelViewSwt.this.onOpenReceipt != null) PurchasesPanelViewSwt.this.onOpenReceipt.accept(current.id); });
                }
            });
        }

        private void paint(GC gc, Rectangle area) {
            var purchase = this.item;
            if (purchase == null) return;

            // Clear full rect first so rounded corners don't expose native
            // widget background on Linux/GTK (SWT.NO_BACKGROUND canvas).
            gc.setBackground(Theme.BG_WHITE);
            gc.fillRectangle(area);

            Surface.drawOutlinedPanel(gc, area);

            // Left blue accent bar
            gc.setBackground(Theme.PRIMARY_BLUE);
            gc.fillRoundRectangle(0, 0, 5, area.height, 4, 4);

            int leftPad = 14;
            int rightPad = 12;

            // Pre-measure right-side elements so we can compute available widths
            // before drawing anything on the left.
            String dateStr = dateFormat.format(new Date(purchase.date));
            gc.setFont(Theme.FONT_BODY);
            Point dateSz = gc.textExtent(dateStr);

            String total = Theme.formatPrice(purchase.total);
            gc.setFont(Theme.FONT_PRICE);
            Point totalSz = gc.textExtent(total);

            // #ID (row 1, left)
            gc.setFont(Theme.FONT_HEADER_BOLD);
            gc.setForeground(Theme.PRIMARY_BLUE);
            gc.drawText("#" + purchase.id, leftPad, 8, true);

            // Date (row 1, right)
            gc.setFont(Theme.FONT_BODY);
            gc.setForeground(Theme.FG_TEXT_SUBTLE);
            gc.drawText(dateStr, area.width - dateSz.x - rightPad, 8, true);

            // Summary (row 2, left) — pixel-based ellipsis to avoid overlap with total
            gc.setFont(Theme.FONT_BODY);
            gc.setForeground(Theme.FG_TEXT_DARK);
            String summary = buildSummary(purchase);
            int maxSummaryW = area.width - totalSz.x - rightPad - leftPad - 8;
            if (maxSummaryW > 0 && gc.textExtent(summary).x > maxSummaryW) {
                int availForText = maxSummaryW - gc.textExtent("\u2026").x;
                while (summary.length() > 0 && gc.textExtent(summary).x > availForText) {
                    summary = summary.substring(0, summary.length() - 1);
                }
                summary += "\u2026";
            }
            gc.drawText(summary, leftPad, 30, true);

            // Total price (row 2, right)
            gc.setFont(Theme.FONT_PRICE);
            gc.setForeground(Theme.FG_TEXT_DARK);
            gc.drawText(total, area.width - totalSz.x - rightPad, 30, true);
        }

        private String buildSummary(PurchaseInfo purchase) {
            if (purchase.items == null || purchase.items.isEmpty()) return "";
            String summary = purchase.items.get(0);
            if (purchase.items.size() > 1) {
                summary += ", " + purchase.items.get(1);
            }
            return summary;
        }
    }

    // ========== SURFACES ==========

    private void paintPagination(GC gc, Rectangle area) {
        Surface.drawPill(gc, area, Theme.BG_PAGE);

        int arrowSize = 28;
        int arrowY = (area.height - arrowSize) / 2;

        // Left arrow
        gc.setFont(Theme.FONT_ICON);
        gc.setForeground(Theme.FG_TEXT_DARK);
        var leftArrow = Theme.ICON_CHEVRON_LEFT;
        var arrowExtent = gc.textExtent(leftArrow);
        int leftX = 4 + (arrowSize - arrowExtent.x) / 2;
        int leftTy = arrowY + (arrowSize - arrowExtent.y) / 2;
        gc.drawText(leftArrow, leftX, leftTy, true);

        // Right arrow
        var rightArrow = Theme.ICON_CHEVRON_RIGHT;
        int rightX = area.width - 4 - arrowSize + (arrowSize - arrowExtent.x) / 2;
        int rightTy = arrowY + (arrowSize - arrowExtent.y) / 2;
        gc.drawText(rightArrow, rightX, rightTy, true);

        // Center: page text on white pill
        int centerW = 48;
        int centerH = 22;
        int cx = (area.width - centerW) / 2;
        int cy = (area.height - centerH) / 2;
        gc.setBackground(Theme.BG_WHITE);
        gc.fillRoundRectangle(cx, cy, centerW, centerH, 12, 12);

        gc.setFont(Theme.FONT_PAGINATION);
        gc.setForeground(Theme.FG_TEXT_DARK);
        int totalPages = getTotalPages();
        var st = this.stateSupplier != null ? this.stateSupplier.get() : null;
        String pageStr = totalPages > 0 && st != null ? (st.page + 1) + " / " + totalPages : "";
        var textExtent = gc.textExtent(pageStr);
        int tx = cx + (centerW - textExtent.x) / 2;
        int ty = cy + (centerH - textExtent.y) / 2;
        gc.drawText(pageStr, tx, ty, true);
    }

    private void updatePagination() {
        if (this.paginationCanvas == null || this.paginationCanvas.isDisposed()) return;
        this.paginationCanvas.redraw();
    }

    private int getTotalPages() {
        var st = this.stateSupplier != null ? this.stateSupplier.get() : null;
        if (st == null || st.pageSize <= 0) return 0;
        return (int) Math.ceil((double) st.totalCount / st.pageSize);
    }
}
