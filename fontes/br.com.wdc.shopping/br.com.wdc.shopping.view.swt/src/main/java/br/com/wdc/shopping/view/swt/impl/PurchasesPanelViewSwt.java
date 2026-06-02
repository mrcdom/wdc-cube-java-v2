package br.com.wdc.shopping.view.swt.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * Purchases panel — list of purchase items with pagination.
 */
public class PurchasesPanelViewSwt extends AbstractViewSwt<PurchasesPanelPresenter> {

    private static final int ITEM_HEIGHT = 50;
    private static final int LIST_SPACING = 8;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private final PurchasesPanelViewState state;

    private boolean notRendered = true;
    private Composite listPanel;
    private Canvas paginationCanvas;
    private int lastHash;
    private boolean capacityComputed;

    public PurchasesPanelViewSwt(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        // Compute capacity from available height
        if (!this.capacityComputed && this.listPanel.getClientArea().height > 0) {
            this.capacityComputed = true;
            int availableHeight = this.listPanel.getClientArea().height;
            int capacity = Math.max(1, (availableHeight + LIST_SPACING) / (ITEM_HEIGHT + LIST_SPACING));
            safeAction("capacityChanged", () -> this.presenter.onItemSizeCapacityChanged(capacity));
        }

        // Rebuild list if data changed
        var purchases = this.state.purchases;
        int hash = purchases != null ? purchases.hashCode() : 0;
        hash = hash * 31 + this.state.page;
        hash = hash * 31 + this.state.totalCount;
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
        this.paginationCanvas = null;
        this.lastHash = 0;
        this.capacityComputed = false;
    }

    private void initialRender() {
        var layout = new GridLayout(1, false);
        layout.marginWidth = 12;
        layout.marginHeight = 12;
        layout.verticalSpacing = 4;
        this.element.setLayout(layout);
        this.element.setBackground(Theme.BG_WHITE);

        // Header with icon + title
        var headerRow = new Composite(this.element, SWT.NONE);
        headerRow.setBackground(Theme.BG_WHITE);
        headerRow.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        var hrLayout = new GridLayout(2, false);
        hrLayout.marginWidth = 0;
        hrLayout.marginHeight = 4;
        hrLayout.horizontalSpacing = 8;
        headerRow.setLayout(hrLayout);

        var histIcon = new Label(headerRow, SWT.NONE);
        histIcon.setFont(Theme.FONT_ICON);
        histIcon.setText(Theme.ICON_CLOCK_HISTORY);
        histIcon.setForeground(Theme.FG_TEXT_DARK);
        histIcon.setBackground(Theme.BG_WHITE);
        histIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        var title = new Label(headerRow, SWT.NONE);
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.FG_TEXT_DARK);
        title.setBackground(Theme.BG_WHITE);
        title.setText("Histórico");
        title.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

        // Subtitle
        var subtitle = new Label(this.element, SWT.NONE);
        subtitle.setFont(Theme.FONT_BODY);
        subtitle.setForeground(Theme.FG_TEXT_SUBTLE);
        subtitle.setBackground(Theme.BG_WHITE);
        subtitle.setText("Toque para ver detalhes");
        subtitle.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        // List area
        this.listPanel = new Composite(this.element, SWT.NONE);
        this.listPanel.setBackground(Theme.BG_WHITE);
        this.listPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        var listLayout = new GridLayout(1, false);
        listLayout.marginWidth = 0;
        listLayout.marginHeight = 0;
        listLayout.verticalSpacing = LIST_SPACING;
        this.listPanel.setLayout(listLayout);

        // Listen for resize to compute capacity
        this.listPanel.addListener(SWT.Resize, _e -> {
            if (this.listPanel.getClientArea().height > 0) {
                int availableHeight = this.listPanel.getClientArea().height;
                int capacity = Math.max(1, (availableHeight + LIST_SPACING) / (ITEM_HEIGHT + LIST_SPACING));
                safeAction("capacityChanged", () -> this.presenter.onItemSizeCapacityChanged(capacity));
            }
        });

        // Pagination footer — pill-shaped container with arrows and page text
        this.paginationCanvas = new Canvas(this.element, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        this.paginationCanvas.setBackground(Theme.BG_WHITE);
        var fGd = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
        fGd.widthHint = 120;
        fGd.heightHint = 36;
        paginationCanvas.setLayoutData(fGd);
        paginationCanvas.setCursor(paginationCanvas.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        paginationCanvas.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = paginationCanvas.getClientArea();
            gc.setAntialias(SWT.ON);

            // Pill container background
            gc.setBackground(Theme.BG_PAGE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, area.height, area.height);

            int arrowSize = 28;
            int arrowY = (area.height - arrowSize) / 2;

            // Left arrow circle area
            gc.setFont(Theme.FONT_ICON);
            gc.setForeground(Theme.FG_TEXT_DARK);
            var leftArrow = Theme.ICON_CHEVRON_LEFT;
            var arrowExtent = gc.textExtent(leftArrow);
            int leftX = 4 + (arrowSize - arrowExtent.x) / 2;
            int leftTy = arrowY + (arrowSize - arrowExtent.y) / 2;
            gc.drawText(leftArrow, leftX, leftTy, true);

            // Right arrow circle area
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
            String pageStr = totalPages > 0 ? (this.state.page + 1) + " / " + totalPages : "";
            var textExtent = gc.textExtent(pageStr);
            int tx = cx + (centerW - textExtent.x) / 2;
            int ty = cy + (centerH - textExtent.y) / 2;
            gc.drawText(pageStr, tx, ty, true);
        });
        paginationCanvas.addListener(SWT.MouseDown, ev -> {
            var area = paginationCanvas.getClientArea();
            int arrowSize = 28;
            if (ev.x < arrowSize + 4) {
                // Left arrow clicked
                int page = this.state.page;
                if (page > 0) {
                    safeAction("prevPage", () -> this.presenter.onPageChange(page - 1));
                }
            } else if (ev.x > area.width - arrowSize - 4) {
                // Right arrow clicked
                int totalPages = getTotalPages();
                int page = this.state.page;
                if (page < totalPages - 1) {
                    safeAction("nextPage", () -> this.presenter.onPageChange(page + 1));
                }
            }
        });
    }

    private void rebuildList(List<PurchaseInfo> purchases) {
        if (this.listPanel == null) return;

        for (var child : this.listPanel.getChildren()) {
            child.dispose();
        }

        if (purchases != null) {
            for (var purchase : purchases) {
                createPurchaseItem(purchase);
            }
        }

        this.listPanel.layout(true, true);
    }

    private void createPurchaseItem(PurchaseInfo purchase) {
        var item = new Canvas(this.listPanel, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = ITEM_HEIGHT;
        item.setLayoutData(gd);
        item.setCursor(item.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        item.addPaintListener(e -> {
            var gc = e.gc;
            var area = item.getClientArea();
            gc.setAntialias(SWT.ON);

            // Card background with rounded corners
            gc.setBackground(Theme.BG_PAGE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 8, 8);

            // Border
            gc.setForeground(Theme.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);

            // Left blue accent bar
            gc.setBackground(Theme.PRIMARY_BLUE);
            gc.fillRoundRectangle(0, 0, 5, area.height, 4, 4);

            int leftPad = 14;

            // #ID in blue bold
            gc.setFont(Theme.FONT_HEADER_BOLD);
            gc.setForeground(Theme.PRIMARY_BLUE);
            String idStr = "#" + purchase.id;
            gc.drawText(idStr, leftPad, 8, true);

            // Product name below ID
            gc.setFont(Theme.FONT_BODY);
            gc.setForeground(Theme.FG_TEXT_DARK);
            String summary = "";
            if (purchase.items != null && !purchase.items.isEmpty()) {
                summary = purchase.items.get(0);
                if (purchase.items.size() > 1) {
                    summary += ", " + purchase.items.get(1);
                }
            }
            if (summary.length() > 30) summary = summary.substring(0, 28) + "...";
            gc.drawText(summary, leftPad, 30, true);

            // Date top-right
            gc.setFont(Theme.FONT_BODY);
            gc.setForeground(Theme.FG_TEXT_SUBTLE);
            String dateStr = dateFormat.format(new Date(purchase.date));
            Point dateSz = gc.textExtent(dateStr);
            gc.drawText(dateStr, area.width - dateSz.x - 12, 8, true);

            // Total price bottom-right
            gc.setFont(Theme.FONT_PRICE);
            gc.setForeground(Theme.FG_TEXT_DARK);
            String total = Theme.formatPrice(purchase.total);
            Point totalSz = gc.textExtent(total);
            gc.drawText(total, area.width - totalSz.x - 12, 30, true);
        });

        item.addListener(SWT.MouseDown, _e -> {
            safeAction("openReceipt", () -> this.presenter.onOpenReceipt(purchase.id));
        });
    }

    private void updatePagination() {
        if (this.paginationCanvas == null || this.paginationCanvas.isDisposed()) return;
        this.paginationCanvas.redraw();
    }

    private int getTotalPages() {
        if (this.state.pageSize <= 0) return 0;
        return (int) Math.ceil((double) this.state.totalCount / this.state.pageSize);
    }
}
