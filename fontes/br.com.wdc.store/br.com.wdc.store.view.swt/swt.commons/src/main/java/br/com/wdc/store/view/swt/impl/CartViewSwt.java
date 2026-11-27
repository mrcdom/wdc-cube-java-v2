package br.com.wdc.store.view.swt.impl;

import static br.com.wdc.store.view.swt.util.GridDataUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.view.swt.AbstractViewSwt;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.components.ErrorBanner;
import br.com.wdc.store.view.swt.theme.Theme;
import br.com.wdc.store.view.swt.util.I18n;
import br.com.wdc.store.view.swt.util.SwtDom;

public class CartViewSwt extends AbstractViewSwt {

    public Supplier<CartScope> scopeSupplier;
    public Runnable onOpenProducts;
    public Runnable onBuy;
    /** The rows' views, in the cart's order: each row is a view of its own ({@link CartItemViewSwt}). */
    public Supplier<List<AbstractViewSwt>> rowViews;

    private boolean notRendered = true;

    // Empty state widgets
    private Composite emptyCard;

    // Filled state widgets
    private Composite filledCard;
    private ErrorBanner errorBanner;
    private Composite itemsContainer;
    private Label totalValueLabel;

    public CartViewSwt(SwtApp app) {
        super("cart", app);
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.emptyCard = null;
        this.filledCard = null;
        this.errorBanner = null;
        this.itemsContainer = null;
        this.totalValueLabel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var scope = this.scopeSupplier != null ? this.scopeSupplier.get() : null;
        var rows = new ArrayList<Composite>();
        if (this.rowViews != null) {
            for (var rowView : this.rowViews.get()) {
                if (this.hold(rowView) != null) {
                    rows.add(rowView.getElement());
                }
            }
        }
        boolean empty = rows.isEmpty();

        // Toggle empty/filled visibility
        setCardVisible(this.emptyCard, empty);
        setCardVisible(this.filledCard, !empty);

        // Sync error banner
        var message = scope != null ? scope.errorMessage.get() : null;
        boolean showError = message != null;
        if (showError) {
            errorBanner.setMessage(I18n.text(message));
        }

        if (showError != errorBanner.isShown()) {
            errorBanner.setShown(showError);
        }

        this.syncRows(rows);
        if (empty) {
            this.totalValueLabel.setText("?");
        } else {
            var total = scope != null && scope.total.get() != null ? scope.total.get() : 0.0;
            var totalValueText = Theme.formatPrice(total);
            if (!Objects.equals(totalValueText, this.totalValueLabel.getText())) {
                this.totalValueLabel.setText(totalValueText);
            }
        }

        this.element.layout(true, true);
    }

    /** Moves the rows' elements into the list, in the cart's order; the ones that left go back offscreen. */
    private void syncRows(List<Composite> rows) {
        for (var child : this.itemsContainer.getChildren()) {
            if (!rows.contains(child) && !child.isDisposed()) {
                child.setParent(this.app.getOffscreen());
            }
        }
        Composite previous = null;
        for (var row : rows) {
            if (row.isDisposed()) {
                continue;
            }
            if (row.getParent() != this.itemsContainer) {
                row.setParent(this.itemsContainer);
                row.setLayoutData(gdFillH(new GridData()));
                row.setVisible(true);
            }
            if (previous == null) {
                row.moveAbove(null);
            } else {
                row.moveBelow(previous);
            }
            previous = row;
        }
    }

    private void setCardVisible(Composite card, boolean visible) {
        if (card == null) return;
        card.setVisible(visible);
        ((GridData) card.getLayoutData()).exclude = !visible;
    }

    private void initialRender() {
        SwtDom.render(this.element, (dom, _root) -> {
            dom.scrolledPage(page -> {
                this.errorBanner = page.errorBanner(16, false, eb -> {});
                renderEmptyCard(page);
                renderFilledCard(page);
            });
        });
    }

    // ========== EMPTY STATE ==========

    private void renderEmptyCard(SwtDom dom) {
        this.emptyCard = dom.card(card -> {
            card.setLayoutData(gdFill(new GridData()));

            dom.cardHeader(Theme.ICON_BAG, I18n.text(StoreMessages.CART_TITLE), I18n.text(StoreMessages.CART_SUBTITLE));
            dom.spacer(40);

            int circleSize = 120;
            dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                var gd = new GridData();
                gdCenter(gd);
                gdGrabH(gd);
                gd.widthHint = circleSize;
                gd.heightHint = circleSize;
                canvas.setLayoutData(gd);
                canvas.addPaintListener(e -> paintEmptyCartIcon(e.gc, circleSize));
            });

            dom.label(SWT.CENTER, lbl -> {
                lbl.setText(I18n.text(StoreMessages.CART_EMPTY));
                lbl.setFont(Theme.FONT_NAV_TITLE);
                lbl.setForeground(Theme.FG_TEXT_DARK);
                lbl.setBackground(Theme.BG_WHITE);
                var lblGd = new GridData();
                gdCenter(lblGd);
                gdGrabH(lblGd);
                gdIndent(lblGd, 0, 16);
                lbl.setLayoutData(lblGd);
            });

            dom.label(SWT.CENTER, lbl -> {
                lbl.setText(I18n.text(StoreMessages.CART_EMPTY_HINT));
                lbl.setFont(Theme.FONT_BODY);
                lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                lbl.setBackground(Theme.BG_WHITE);
                var lblGd = new GridData();
                gdCenter(lblGd);
                gdGrabH(lblGd);
                gdIndent(lblGd, 0, 6);
                lbl.setLayoutData(lblGd);
            });

            dom.primaryButton(Theme.ICON_GRID_3X3_GAP, I18n.text(StoreMessages.CART_VIEW_PRODUCTS), btn -> {
                var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
                gd.verticalIndent = 20;
                btn.setLayoutData(gd);
                btn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", () -> { if (onOpenProducts != null) onOpenProducts.run(); }));
            });

            dom.spacer(40);
        });
    }

    // ========== FILLED STATE ==========

    private void renderFilledCard(SwtDom dom) {
        this.filledCard = dom.card(card -> {
            var cardGd = new GridData();
            gdFillH(cardGd);
            gdTop(cardGd);
            card.setLayoutData(cardGd);

            dom.cardHeader(Theme.ICON_BAG, I18n.text(StoreMessages.CART_TITLE), I18n.text(StoreMessages.CART_SUBTITLE));

            // Items container — slots will be synced here
            this.itemsContainer = dom.col(col -> {
                col.setBackground(Theme.BG_WHITE);
                col.setLayoutData(gdFillH(new GridData()));
            });

            // Total row
            dom.separator(16);
            dom.row(2, totalRow -> {
                var totalRowGd = new GridData();
                gdFillH(totalRowGd);
                gdIndent(totalRowGd, 0, 16);
                totalRow.setLayoutData(totalRowGd);
                totalRow.setBackground(Theme.BG_WHITE);
                var rowLayout = (GridLayout) totalRow.getLayout();
                rowLayout.horizontalSpacing = 12;

                dom.label(lbl -> {
                    lbl.setText(I18n.text(StoreMessages.CART_TOTAL));
                    lbl.setFont(Theme.FONT_BODY);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setLayoutData(gdFillH(new GridData()));
                });

                this.totalValueLabel = dom.label(SWT.RIGHT, lbl -> {
                    lbl.setFont(Theme.FONT_PRICE);
                    lbl.setForeground(Theme.PRIMARY_BLUE);
                    lbl.setBackground(Theme.BG_WHITE);
                    lbl.setLayoutData(gdRight(new GridData()));
                });
            });

            // Actions row
            dom.row(2, actionsRow -> {
                var actionsRowGd = new GridData();
                gdFillH(actionsRowGd);
                gdIndent(actionsRowGd, 0, 24);
                actionsRow.setLayoutData(actionsRowGd);
                actionsRow.setBackground(Theme.BG_WHITE);

                dom.actionButton(Theme.ICON_ARROW_LEFT, I18n.text(StoreMessages.CART_CONTINUE_SHOPPING), Theme.BG_WHITE, btn -> {
                    btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", () -> { if (onOpenProducts != null) onOpenProducts.run(); }));
                });

                dom.primaryButton(Theme.ICON_CHECK2_SQUARE, I18n.text(StoreMessages.CART_CHECKOUT), btn -> {
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onBuy", () -> { if (onBuy != null) onBuy.run(); }));
                });
            });
        });
    }

    // ========== SURFACES ==========

    private void paintEmptyCartIcon(GC gc, int size) {
        gc.setAntialias(SWT.ON);
        gc.setBackground(Theme.BG_WHITE);
        gc.fillRectangle(0, 0, size, size);
        gc.setBackground(Theme.BG_ICON_BOX);
        gc.fillOval(0, 0, size, size);
        gc.setFont(Theme.FONT_ICON_LARGE);
        gc.setForeground(Theme.PRIMARY_BLUE);
        var ext = gc.textExtent(Theme.ICON_BAG);
        gc.drawText(Theme.ICON_BAG, (size - ext.x) / 2, (size - ext.y) / 2, true);
    }
}
