package br.com.wdc.store.view.swt.impl;

import static br.com.wdc.store.view.swt.util.GridDataUtils.*;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope.CartItemScope;
import br.com.wdc.store.view.swt.AbstractViewSwt;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.theme.Theme;
import br.com.wdc.store.view.swt.util.SwtDom;

/**
 * The view of a cart row: a segment of the cart with its own scope and actions (the item is the row — no product id
 * goes back). Its element is born offscreen; the cart moves it into its list, in the cart's order.
 */
public class CartItemViewSwt extends AbstractViewSwt {

    public Supplier<CartItemScope> scopeSupplier;
    public IntConsumer onModifyQuantity;
    public Runnable onRemove;

    private boolean notRendered = true;
    private Canvas nameCanvas;
    private String nameToDraw = "";
    private Label qtyLabel;
    private Label priceLabel;

    public CartItemViewSwt(SwtApp app) {
        super("cart-item", app);
        this.element.setBackground(Theme.BG_WHITE);

        var layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        this.element.setLayout(layout);
    }

    private int quantity() {
        var row = this.scopeSupplier != null ? this.scopeSupplier.get() : null;
        var q = row != null ? row.quantity.get() : null;
        return q != null ? q : 0;
    }

    @Override
    public void doUpdate() {
        var changed = false;
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
            changed = true;
        }

        var row = this.scopeSupplier != null ? this.scopeSupplier.get() : null;
        if (row != null) {
            var name = row.name.get() != null ? row.name.get() : "";
            if (!name.equals(this.nameToDraw)) {
                this.nameToDraw = name;
                this.nameCanvas.redraw();
            }

            var qtyText = String.valueOf(quantity());
            if (!qtyText.equals(this.qtyLabel.getText())) {
                this.qtyLabel.setText(qtyText);
                changed = true;
            }

            var price = row.price.get() != null ? row.price.get() : 0.0;
            var priceText = Theme.formatPrice(price * quantity());
            if (!priceText.equals(this.priceLabel.getText())) {
                this.priceLabel.setText(priceText);
                changed = true;
            }
        }

        // The cart may have placed this row in its list before the row drew itself (an empty row measures zero):
        // whatever changed the row's size, its ancestors lay out again
        if (changed) {
            this.relayoutAncestors();
        }
    }

    @Override
    public void release() {
        // The presenter releases the row from its own thread; SWT widgets are disposed on the UI thread
        var display = this.app.getDisplay();
        Runnable dispose = () -> {
            if (!this.element.isDisposed()) {
                this.element.dispose();
            }
        };
        if (display.getThread() == Thread.currentThread()) {
            dispose.run();
        } else if (!display.isDisposed()) {
            display.asyncExec(dispose);
        }
    }

    private void initialRender() {
        SwtDom.render(this.element, (dom, _self) -> {
            dom.separator(16);

            dom.row(6, row -> {
                var rowGd = new GridData();
                gdFillH(rowGd);
                gdIndent(rowGd, 0, 12);
                row.setLayoutData(rowGd);
                row.setBackground(Theme.BG_WHITE);
                var rowLayout = (GridLayout) row.getLayout();
                rowLayout.horizontalSpacing = 8;

                this.nameCanvas = dom.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, canvas -> {
                    // A canvas has no natural height (it would measure 64): the row is as tall as its buttons
                    var nameGd = gdFillH(new GridData());
                    nameGd.heightHint = 30;
                    canvas.setLayoutData(nameGd);
                    canvas.addPaintListener(e -> {
                        var gc = e.gc;
                        var bounds = canvas.getClientArea();
                        gc.setBackground(Theme.BG_WHITE);
                        gc.fillRectangle(bounds);
                        gc.setFont(Theme.FONT_BODY);
                        gc.setForeground(Theme.FG_TEXT_DARK);
                        var text = this.nameToDraw;
                        if (text != null && !text.isEmpty() && bounds.width > 0) {
                            var ellipsis = "\u2026"; // …
                            var extent = gc.textExtent(text);
                            if (extent.x > bounds.width) {
                                var ellipsisW = gc.textExtent(ellipsis).x;
                                while (text.length() > 0 && gc.textExtent(text).x + ellipsisW > bounds.width) {
                                    text = text.substring(0, text.length() - 1);
                                }
                                text += ellipsis;
                            }
                            int textY = (bounds.height - extent.y) / 2;
                            gc.drawText(text, 0, Math.max(0, textY), true);
                        }
                    });
                });

                dom.iconButton(Theme.ICON_DASH, Theme.BG_WHITE, btn -> {
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
                        if (this.onModifyQuantity != null)
                            this.onModifyQuantity.accept(quantity() - 1);
                    }));
                });

                this.qtyLabel = dom.label(SWT.CENTER, lbl -> {
                    lbl.setFont(Theme.FONT_BODY_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_WHITE);
                    var lblGd = new GridData();
                    gdCenter(lblGd);
                    gdWidth(lblGd, 30);
                    lbl.setLayoutData(lblGd);
                });

                dom.iconButton(Theme.ICON_PLUS, Theme.BG_WHITE, btn -> {
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
                        if (this.onModifyQuantity != null)
                            this.onModifyQuantity.accept(quantity() + 1);
                    }));
                });

                this.priceLabel = dom.label(SWT.RIGHT, lbl -> {
                    lbl.setFont(Theme.FONT_PRICE);
                    lbl.setForeground(Theme.PRIMARY_BLUE);
                    lbl.setBackground(Theme.BG_WHITE);
                    var lblGd = new GridData();
                    gdRight(lblGd);
                    gdWidth(lblGd, 100);
                    lbl.setLayoutData(lblGd);
                });

                dom.iconButton(Theme.ICON_X, Theme.FG_PRICE, Theme.FONT_ICON_NAV,
                        Theme.BG_WHITE, btn -> {
                            btn.addListener(SWT.MouseUp, evt -> safeAction("onRemove", () -> {
                                if (this.onRemove != null)
                                    this.onRemove.run();
                            }));
                        });
            });
        });
    }
}
