package br.com.wdc.shopping.view.swt.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.framework.commons.function.ThrowingBiConsumer;
import br.com.wdc.framework.commons.function.ThrowingSupplier;
import java.util.function.LongConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.components.ErrorBanner;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

public class CartViewSwt extends AbstractViewSwt {

    public Supplier<CartViewState> stateSupplier;
    public Runnable onOpenProducts;
    public Runnable onBuy;
    public BiConsumer<Long, Integer> onModifyQuantity;
    public LongConsumer onRemoveProduct;

    private boolean notRendered = true;

    // Empty state widgets
    private Composite emptyCard;

    // Filled state widgets
    private Composite filledCard;
    private ErrorBanner errorBanner;
    private Composite itemsContainer;
    private final List<CartItemSlot> itemSlots = new ArrayList<>();
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
        this.itemSlots.clear();
        this.totalValueLabel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var state = this.stateSupplier != null ? this.stateSupplier.get() : null;
        var items = state != null ? state.items : null;
        boolean empty = items == null || items.isEmpty();

        // Toggle empty/filled visibility
        setCardVisible(this.emptyCard, empty);
        setCardVisible(this.filledCard, !empty);

        // Sync error banner
        boolean showError = state != null && state.errorMessage != null && !state.errorMessage.isEmpty();
        if (showError) {
            errorBanner.setMessage(state.errorMessage);
        }

        if (showError != errorBanner.isShown()) {
            errorBanner.setShown(showError);
        }

        if (empty) {
            Supplier<CartItemSlot> factory = ThrowingSupplier.noop();
            BiConsumer<CartItemSlot, CartItem> updater = ThrowingBiConsumer.noop();
            syncList(this.itemsContainer, Collections.emptyList(), this.itemSlots, factory, updater);
            this.totalValueLabel.setText("?");
        } else {
            syncList(this.itemsContainer, items, this.itemSlots,
                    () -> new CartItemSlot(this.itemsContainer),
                    (slot, item) -> { slot.item = item; slot.doUpdate(); });
            double total = 0;
            for (var item : items) { total += item.price * item.quantity; }
            var totalValueText = Theme.formatPrice(total);
            if (!Objects.equals(totalValueText, this.totalValueLabel.getText())) {
                this.totalValueLabel.setText(totalValueText);
            }
        }

        this.element.layout(true, true);
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

            dom.cardHeader(Theme.ICON_BAG, "Carrinho", "Seus produtos selecionados");
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
                lbl.setText("Carrinho vazio");
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
                lbl.setText("Adicione produtos para começar");
                lbl.setFont(Theme.FONT_BODY);
                lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                lbl.setBackground(Theme.BG_WHITE);
                var lblGd = new GridData();
                gdCenter(lblGd);
                gdGrabH(lblGd);
                gdIndent(lblGd, 0, 6);
                lbl.setLayoutData(lblGd);
            });

            dom.primaryButton(Theme.ICON_GRID_3X3_GAP, "Ver produtos", btn -> {
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

            dom.cardHeader(Theme.ICON_BAG, "Carrinho", "Seus produtos selecionados");

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
                    lbl.setText("Total:");
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

                dom.actionButton(Theme.ICON_ARROW_LEFT, "Continuar comprando", Theme.BG_WHITE, btn -> {
                    btn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", () -> { if (onOpenProducts != null) onOpenProducts.run(); }));
                });

                dom.primaryButton(Theme.ICON_CHECK2_SQUARE, "Finalizar pedido", btn -> {
                    btn.addListener(SWT.MouseUp, evt -> safeAction("onBuy", () -> { if (onBuy != null) onBuy.run(); }));
                });
            });
        });
    }

    // ========== CART ITEM SLOT ==========

    private class CartItemSlot extends Composite {

        CartItem item;

        private boolean notRendered = true;
        private Canvas nameCanvas;
        private String nameToDraw = "";
        private Label qtyLabel;
        private Label priceLabel;

        CartItemSlot(Composite parent) {
            super(parent, SWT.NONE);
            setBackground(Theme.BG_WHITE);
            setLayoutData(gdFillH(new GridData()));

            var layout = new GridLayout(1, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.verticalSpacing = 0;
            setLayout(layout);
        }

        void doUpdate() {
            if (this.notRendered) {
                initialRender();
                this.notRendered = false;
            }

            var current = this.item;
            if (!current.name.equals(this.nameToDraw)) {
                this.nameToDraw = current.name;
                this.nameCanvas.redraw();
            }

            var qtyText = String.valueOf(current.quantity);
            if (!qtyText.equals(this.qtyLabel.getText())) {
                this.qtyLabel.setText(qtyText);
            }

            var priceText = Theme.formatPrice(current.price * current.quantity);
            if (!priceText.equals(this.priceLabel.getText())) {
                this.priceLabel.setText(priceText);
            }
        }

        private void initialRender() {
            SwtDom.render(this, (dom, _self) -> {
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
                        canvas.setLayoutData(gdFillH(new GridData()));
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
                            if (CartViewSwt.this.onModifyQuantity != null)
                                CartViewSwt.this.onModifyQuantity.accept(item.id, item.quantity - 1);
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
                            if (CartViewSwt.this.onModifyQuantity != null)
                                CartViewSwt.this.onModifyQuantity.accept(item.id, item.quantity + 1);
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
                                btn.addListener(SWT.MouseUp, evt -> safeAction("onRemoveProduct", () -> {
                                    if (CartViewSwt.this.onRemoveProduct != null)
                                        CartViewSwt.this.onRemoveProduct.accept(item.id);
                                }));
                            });
                });
            });
        }
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
