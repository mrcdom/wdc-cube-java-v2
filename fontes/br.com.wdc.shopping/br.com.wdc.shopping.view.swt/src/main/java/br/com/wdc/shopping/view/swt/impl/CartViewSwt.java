package br.com.wdc.shopping.view.swt.impl;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.components.ActionButton;
import br.com.wdc.shopping.view.swt.components.CardHeader;
import br.com.wdc.shopping.view.swt.components.ErrorBanner;
import br.com.wdc.shopping.view.swt.components.IconButton;
import br.com.wdc.shopping.view.swt.components.PrimaryButton;
import br.com.wdc.shopping.view.swt.components.ScrolledPage;
import br.com.wdc.shopping.view.swt.components.Separator;
import br.com.wdc.shopping.view.swt.components.ShadowCard;
import br.com.wdc.shopping.view.swt.theme.Theme;

public class CartViewSwt extends AbstractViewSwt<CartPresenter> {

    private final CartViewState state;

    public CartViewSwt(CartPresenter presenter) {
        super("cart", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        for (var child : this.element.getChildren()) {
            child.dispose();
        }
        render();
    }

    private void render() {
        var page = new ScrolledPage(this.element);
        var content = page.getContent();

        var items = state.items;
        boolean empty = items == null || items.isEmpty();

        if (empty) {
            renderEmptyCard(content);
        } else {
            renderFilledCard(content, items);
        }

        page.complete();
    }

    // ========== EMPTY STATE ==========

    private void renderEmptyCard(Composite parent) {
        var card = new ShadowCard(parent);
        card.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        new CardHeader(card, Theme.ICON_BAG, "Carrinho", "Seus produtos selecionados");

        // Spacer top
        var spacerTop = new Label(card, SWT.NONE);
        spacerTop.setBackground(Theme.BG_WHITE);
        var spacerTopGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        spacerTopGd.heightHint = 40;
        spacerTop.setLayoutData(spacerTopGd);

        // Large circle with bag icon
        int circleSize = 120;
        var iconCircle = new Canvas(card, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var circleGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        circleGd.widthHint = circleSize;
        circleGd.heightHint = circleSize;
        iconCircle.setLayoutData(circleGd);
        iconCircle.addPaintListener(e -> paintEmptyCartIcon(e.gc, circleSize));

        // "Carrinho vazio"
        var emptyTitle = new Label(card, SWT.CENTER);
        emptyTitle.setText("Carrinho vazio");
        emptyTitle.setFont(Theme.FONT_NAV_TITLE);
        emptyTitle.setForeground(Theme.FG_TEXT_DARK);
        emptyTitle.setBackground(Theme.BG_WHITE);
        var emptyTitleGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        emptyTitleGd.verticalIndent = 16;
        emptyTitle.setLayoutData(emptyTitleGd);

        // Subtitle
        var emptySubtitle = new Label(card, SWT.CENTER);
        emptySubtitle.setText("Adicione produtos para começar");
        emptySubtitle.setFont(Theme.FONT_BODY);
        emptySubtitle.setForeground(Theme.FG_TEXT_SUBTLE);
        emptySubtitle.setBackground(Theme.BG_WHITE);
        var emptySubGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        emptySubGd.verticalIndent = 6;
        emptySubtitle.setLayoutData(emptySubGd);

        // "Ver produtos" button
        var viewBtn = new PrimaryButton(card, Theme.ICON_GRID_3X3_GAP, "Ver produtos");
        var viewBtnGd = (GridData) viewBtn.getLayoutData();
        viewBtnGd.horizontalAlignment = SWT.CENTER;
        viewBtnGd.verticalIndent = 20;
        viewBtn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", presenter::onOpenProducts));

        // Spacer bottom
        var spacerBottom = new Label(card, SWT.NONE);
        spacerBottom.setBackground(Theme.BG_WHITE);
        var spacerBottomGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        spacerBottomGd.heightHint = 40;
        spacerBottom.setLayoutData(spacerBottomGd);
    }

    // ========== CART WITH ITEMS ==========

    private void renderFilledCard(Composite parent, List<CartItem> items) {
        var card = new ShadowCard(parent);
        card.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        new CardHeader(card, Theme.ICON_BAG, "Carrinho", "Seus produtos selecionados");

        // Error banner (if any)
        if (state.errorMessage != null && !state.errorMessage.isEmpty()) {
            var banner = new ErrorBanner(card, 16);
            banner.setMessage(state.errorMessage);
        }

        // Items
        for (var item : items) {
            renderCartItem(card, item);
        }

        // Total row
        renderTotalRow(card, items);

        // Actions row
        renderActionsRow(card);

        card.layout(true, true);
    }

    private void renderCartItem(Composite card, CartItem item) {
        new Separator(card, 16);

        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 12;
        row.setLayoutData(rowGd);
        row.setBackground(Theme.BG_WHITE);

        var rowLayout = new GridLayout(6, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 8;
        row.setLayout(rowLayout);

        // Product name
        var nameLabel = new Label(row, SWT.NONE);
        nameLabel.setText(item.name);
        nameLabel.setFont(Theme.FONT_BODY);
        nameLabel.setForeground(Theme.FG_TEXT_DARK);
        nameLabel.setBackground(Theme.BG_WHITE);
        nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Minus button
        var minusBtn = new IconButton(row, Theme.ICON_DASH, Theme.BG_WHITE);
        minusBtn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
            presenter.onModifyQuantity(item.id, item.quantity - 1);
        }));

        // Quantity value
        var qtyLabel = new Label(row, SWT.CENTER);
        qtyLabel.setText(String.valueOf(item.quantity));
        qtyLabel.setFont(Theme.FONT_BODY_BOLD);
        qtyLabel.setForeground(Theme.FG_TEXT_DARK);
        qtyLabel.setBackground(Theme.BG_WHITE);
        var qtyGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        qtyGd.widthHint = 30;
        qtyLabel.setLayoutData(qtyGd);

        // Plus button
        var plusBtn = new IconButton(row, Theme.ICON_PLUS, Theme.BG_WHITE);
        plusBtn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
            presenter.onModifyQuantity(item.id, item.quantity + 1);
        }));

        // Price
        var priceLabel = new Label(row, SWT.RIGHT);
        priceLabel.setText(Theme.formatPrice(item.price * item.quantity));
        priceLabel.setFont(Theme.FONT_PRICE);
        priceLabel.setForeground(Theme.PRIMARY_BLUE);
        priceLabel.setBackground(Theme.BG_WHITE);
        var priceGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        priceGd.widthHint = 100;
        priceLabel.setLayoutData(priceGd);

        // Remove (X) button
        var removeBtn = new IconButton(row, Theme.ICON_X, Theme.FG_PRICE, Theme.FONT_ICON_NAV, Theme.BG_WHITE);
        removeBtn.addListener(SWT.MouseUp, evt -> safeAction("onRemoveProduct", () -> {
            presenter.onRemoveProduct(item.id);
        }));
    }

    private void renderTotalRow(Composite card, List<CartItem> items) {
        new Separator(card, 16);

        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 16;
        row.setLayoutData(rowGd);
        row.setBackground(Theme.BG_WHITE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.horizontalSpacing = 12;
        row.setLayout(rowLayout);

        var totalLabel = new Label(row, SWT.NONE);
        totalLabel.setText("Total:");
        totalLabel.setFont(Theme.FONT_BODY);
        totalLabel.setForeground(Theme.FG_TEXT_DARK);
        totalLabel.setBackground(Theme.BG_WHITE);
        totalLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        double total = 0;
        for (var item : items) {
            total += item.price * item.quantity;
        }

        var totalValue = new Label(row, SWT.NONE);
        totalValue.setText(Theme.formatPrice(total));
        totalValue.setFont(Theme.FONT_PRICE);
        totalValue.setForeground(Theme.PRIMARY_BLUE);
        totalValue.setBackground(Theme.BG_WHITE);
        totalValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    }

    private void renderActionsRow(Composite card) {
        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 24;
        row.setLayoutData(rowGd);
        row.setBackground(Theme.BG_WHITE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        row.setLayout(rowLayout);

        // "← Continuar comprando"
        var backBtn = new ActionButton(row, Theme.ICON_ARROW_LEFT, "Continuar comprando", Theme.BG_WHITE);
        ((GridData) backBtn.getLayoutData()).grabExcessHorizontalSpace = true;
        backBtn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", presenter::onOpenProducts));

        // "Finalizar pedido"
        var buyBtn = new PrimaryButton(row, Theme.ICON_CHECK2_SQUARE, "Finalizar pedido");
        buyBtn.addListener(SWT.MouseUp, evt -> safeAction("onBuy", presenter::onBuy));
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
