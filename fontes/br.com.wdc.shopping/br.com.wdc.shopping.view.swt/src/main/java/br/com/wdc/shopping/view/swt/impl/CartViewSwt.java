package br.com.wdc.shopping.view.swt.impl;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.Styles;

public class CartViewSwt extends AbstractViewSwt<CartPresenter> {

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Icons (Bootstrap Icons codepoints)
    private static final String ICON_BAG = "\uF179";
    private static final String ICON_DASH = "\uF2EA";
    private static final String ICON_PLUS = "\uF4FE";
    private static final String ICON_X = "\uF62A";
    private static final String ICON_ARROW_LEFT = "\uF12F";
    private static final String ICON_CHECK2_SQUARE = "\uF26E";
    private static final String ICON_GRID_3X3_GAP = "\uF3EE";

    // Colors
    private static final Color BG_ICON_BOX = new Color(0xE8, 0xF1, 0xFC);
    private static final Color BG_BTN_HOVER = new Color(0xEE, 0xEE, 0xEE);

    private final CartViewState state;
    private boolean notRendered = true;

    public CartViewSwt(CartPresenter presenter) {
        super("cart", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        // Dispose all children and re-render (cart items change on each update)
        for (var child : this.element.getChildren()) {
            child.dispose();
        }
        render();
    }

    private void render() {
        var root = this.element;
        root.setBackground(Styles.BG_PAGE);
        root.setLayout(new GridLayout(1, false));

        var scrolled = new ScrolledComposite(root, SWT.V_SCROLL);
        scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(false);
        scrolled.setBackground(Styles.BG_PAGE);

        var content = new Composite(scrolled, SWT.NONE);
        content.setBackground(Styles.BG_PAGE);

        var contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = 20;
        contentLayout.marginHeight = 20;
        content.setLayout(contentLayout);

        var items = state.items;
        boolean empty = items == null || items.isEmpty();

        if (empty) {
            renderEmptyCard(content);
        } else {
            renderFilledCard(content, items);
        }

        scrolled.setContent(content);
        content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        root.layout(true, true);
    }

    // ========== EMPTY STATE ==========

    private void renderEmptyCard(Composite parent) {
        // Card with shadow (same as ReceiptViewSwt)
        var card = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        card.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        var cardLayout = new GridLayout(1, false);
        cardLayout.marginWidth = 32;
        cardLayout.marginHeight = 32;
        cardLayout.verticalSpacing = 0;
        card.setLayout(cardLayout);

        card.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = card.getClientArea();

            gc.setBackground(Styles.BG_PAGE);
            gc.fillRectangle(area);

            int x = 4, y = 2, w = area.width - 8, h = area.height - 6;

            // Shadow
            gc.setAlpha(15);
            gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            gc.fillRoundRectangle(x, y + 2, w, h, 24, 24);
            gc.setAlpha(10);
            gc.fillRoundRectangle(x - 1, y + 1, w + 2, h + 2, 26, 26);
            gc.setAlpha(255);

            // White card fill
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRoundRectangle(x, y, w, h, 24, 24);

            // Border
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(x, y, w - 1, h - 1, 24, 24);
        });

        // Header
        renderCardHeader(card, "Seus produtos selecionados");

        // Spacer top
        var spacerTop = new Label(card, SWT.NONE);
        spacerTop.setBackground(Styles.BG_WHITE);
        var spacerTopGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        spacerTopGd.heightHint = 40;
        spacerTop.setLayoutData(spacerTopGd);

        // Large circle with bag icon (centered)
        int circleSize = 120;
        var iconCircle = new Canvas(card, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var circleGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        circleGd.widthHint = circleSize;
        circleGd.heightHint = circleSize;
        iconCircle.setLayoutData(circleGd);
        iconCircle.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRectangle(0, 0, circleSize, circleSize);
            gc.setBackground(BG_ICON_BOX);
            gc.fillOval(0, 0, circleSize, circleSize);
            gc.setFont(Styles.FONT_ICON_LARGE);
            gc.setForeground(Styles.PRIMARY_BLUE);
            var ext = gc.textExtent(ICON_BAG);
            gc.drawText(ICON_BAG, (circleSize - ext.x) / 2, (circleSize - ext.y) / 2, true);
        });

        // "Carrinho vazio" title (centered)
        var emptyTitle = new Label(card, SWT.CENTER);
        emptyTitle.setText("Carrinho vazio");
        emptyTitle.setFont(Styles.FONT_NAV_TITLE);
        emptyTitle.setForeground(Styles.FG_TEXT_DARK);
        emptyTitle.setBackground(Styles.BG_WHITE);
        var emptyTitleGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        emptyTitleGd.verticalIndent = 16;
        emptyTitle.setLayoutData(emptyTitleGd);

        // "Adicione produtos para começar" subtitle (centered)
        var emptySubtitle = new Label(card, SWT.CENTER);
        emptySubtitle.setText("Adicione produtos para começar");
        emptySubtitle.setFont(Styles.FONT_BODY);
        emptySubtitle.setForeground(Styles.FG_TEXT_SUBTLE);
        emptySubtitle.setBackground(Styles.BG_WHITE);
        var emptySubGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        emptySubGd.verticalIndent = 6;
        emptySubtitle.setLayoutData(emptySubGd);

        // "Ver produtos" primary button (centered)
        var viewBtn = createPrimaryButton(card, ICON_GRID_3X3_GAP, "Ver produtos");
        var viewBtnGd = (GridData) viewBtn.getLayoutData();
        viewBtnGd.horizontalAlignment = SWT.CENTER;
        viewBtnGd.verticalIndent = 20;
        viewBtn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", () -> presenter.onOpenProducts()));

        // Spacer bottom
        var spacerBottom = new Label(card, SWT.NONE);
        spacerBottom.setBackground(Styles.BG_WHITE);
        var spacerBottomGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        spacerBottomGd.heightHint = 40;
        spacerBottom.setLayoutData(spacerBottomGd);
    }

    // ========== CART WITH ITEMS ==========

    private void renderFilledCard(Composite parent, List<CartItem> items) {
        // Card with shadow (same as ReceiptViewSwt)
        var card = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        card.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        var cardLayout = new GridLayout(1, false);
        cardLayout.marginWidth = 32;
        cardLayout.marginHeight = 32;
        cardLayout.verticalSpacing = 0;
        card.setLayout(cardLayout);

        card.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = card.getClientArea();

            gc.setBackground(Styles.BG_PAGE);
            gc.fillRectangle(area);

            int x = 4, y = 2, w = area.width - 8, h = area.height - 6;

            // Shadow
            gc.setAlpha(15);
            gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            gc.fillRoundRectangle(x, y + 2, w, h, 24, 24);
            gc.setAlpha(10);
            gc.fillRoundRectangle(x - 1, y + 1, w + 2, h + 2, 26, 26);
            gc.setAlpha(255);

            // White card fill
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRoundRectangle(x, y, w, h, 24, 24);

            // Border
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(x, y, w - 1, h - 1, 24, 24);
        });

        // Header
        renderCardHeader(card, "Seus produtos selecionados");

        // Error banner (if any) — between header and items
        if (state.errorMessage != null && !state.errorMessage.isEmpty()) {
            renderErrorBanner(card);
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

    private void renderCardHeader(Composite card, String subtitle) {
        var header = new Composite(card, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        header.setBackground(Styles.BG_WHITE);

        var headerLayout = new GridLayout(2, false);
        headerLayout.marginWidth = 0;
        headerLayout.marginHeight = 0;
        headerLayout.horizontalSpacing = 10;
        header.setLayout(headerLayout);

        // Icon box: 40x40, bg #e8f1fc, border-radius 10px (same as receipt)
        var iconBox = new Canvas(header, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var iconGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        iconGd.widthHint = 40;
        iconGd.heightHint = 40;
        iconBox.setLayoutData(iconGd);
        iconBox.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRectangle(0, 0, 40, 40);
            gc.setBackground(BG_ICON_BOX);
            gc.fillRoundRectangle(0, 0, 40, 40, 10, 10);
            gc.setFont(Styles.FONT_ICON);
            gc.setForeground(Styles.PRIMARY_BLUE);
            var ext = gc.textExtent(ICON_BAG);
            gc.drawText(ICON_BAG, (40 - ext.x) / 2, (40 - ext.y) / 2, true);
        });

        // Title block
        var titleBlock = new Composite(header, SWT.NONE);
        titleBlock.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleBlock.setBackground(Styles.BG_WHITE);

        var titleLayout = new GridLayout(1, false);
        titleLayout.marginWidth = 0;
        titleLayout.marginHeight = 0;
        titleLayout.verticalSpacing = 2;
        titleBlock.setLayout(titleLayout);

        var titleLabel = new Label(titleBlock, SWT.NONE);
        titleLabel.setText("Carrinho");
        titleLabel.setFont(Styles.FONT_TITLE);
        titleLabel.setForeground(Styles.FG_TEXT_DARK);
        titleLabel.setBackground(Styles.BG_WHITE);

        var subtitleLabel = new Label(titleBlock, SWT.NONE);
        subtitleLabel.setText(subtitle);
        subtitleLabel.setFont(Styles.FONT_BANNER_SUBTITLE);
        subtitleLabel.setForeground(Styles.FG_TEXT_SUBTLE);
        subtitleLabel.setBackground(Styles.BG_WHITE);
    }

    private void renderCartItem(Composite card, CartItem item) {
        // Separator
        var sep = new Canvas(card, SWT.NONE);
        var sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sepGd.heightHint = 1;
        sepGd.verticalIndent = 16;
        sep.setLayoutData(sepGd);
        sep.setBackground(Styles.BG_WHITE);
        sep.addPaintListener(e -> {
            e.gc.setBackground(Styles.BORDER_LIGHT);
            e.gc.fillRectangle(0, 0, sep.getBounds().width, 1);
        });

        // Item row: name | - qty + | price | X
        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 12;
        row.setLayoutData(rowGd);
        row.setBackground(Styles.BG_WHITE);

        var rowLayout = new GridLayout(6, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 8;
        row.setLayout(rowLayout);

        // Product name
        var nameLabel = new Label(row, SWT.NONE);
        nameLabel.setText(item.name);
        nameLabel.setFont(Styles.FONT_BODY);
        nameLabel.setForeground(Styles.FG_TEXT_DARK);
        nameLabel.setBackground(Styles.BG_WHITE);
        nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Minus button
        var minusBtn = createSmallIconButton(row, ICON_DASH);
        minusBtn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
            presenter.onModifyQuantity(item.id, item.quantity - 1);
        }));

        // Quantity value
        var qtyLabel = new Label(row, SWT.CENTER);
        qtyLabel.setText(String.valueOf(item.quantity));
        qtyLabel.setFont(Styles.FONT_BODY_BOLD);
        qtyLabel.setForeground(Styles.FG_TEXT_DARK);
        qtyLabel.setBackground(Styles.BG_WHITE);
        var qtyGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        qtyGd.widthHint = 30;
        qtyLabel.setLayoutData(qtyGd);

        // Plus button
        var plusBtn = createSmallIconButton(row, ICON_PLUS);
        plusBtn.addListener(SWT.MouseUp, evt -> safeAction("onModifyQuantity", () -> {
            presenter.onModifyQuantity(item.id, item.quantity + 1);
        }));

        // Price
        var priceLabel = new Label(row, SWT.RIGHT);
        double itemTotal = item.price * item.quantity;
        priceLabel.setText(PRICE_FORMAT.format(itemTotal));
        priceLabel.setFont(Styles.FONT_PRICE);
        priceLabel.setForeground(Styles.PRIMARY_BLUE);
        priceLabel.setBackground(Styles.BG_WHITE);
        var priceGd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        priceGd.widthHint = 100;
        priceLabel.setLayoutData(priceGd);

        // Remove (X) button — red, slightly larger icon, no border unless hovered
        var removeBtn = createRemoveButton(row);
        removeBtn.addListener(SWT.MouseUp, evt -> safeAction("onRemoveProduct", () -> {
            presenter.onRemoveProduct(item.id);
        }));
    }

    private void renderTotalRow(Composite card, List<CartItem> items) {
        // Separator
        var sep = new Canvas(card, SWT.NONE);
        var sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sepGd.heightHint = 1;
        sepGd.verticalIndent = 16;
        sep.setLayoutData(sepGd);
        sep.setBackground(Styles.BG_WHITE);
        sep.addPaintListener(e -> {
            e.gc.setBackground(Styles.BORDER_LIGHT);
            e.gc.fillRectangle(0, 0, sep.getBounds().width, 1);
        });

        // Total row aligned right
        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 16;
        row.setLayoutData(rowGd);
        row.setBackground(Styles.BG_WHITE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.horizontalSpacing = 12;
        row.setLayout(rowLayout);

        var totalLabel = new Label(row, SWT.NONE);
        totalLabel.setText("Total:");
        totalLabel.setFont(Styles.FONT_BODY);
        totalLabel.setForeground(Styles.FG_TEXT_DARK);
        totalLabel.setBackground(Styles.BG_WHITE);
        totalLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        double total = 0;
        for (var item : items) {
            total += item.price * item.quantity;
        }

        var totalValue = new Label(row, SWT.NONE);
        totalValue.setText(PRICE_FORMAT.format(total));
        totalValue.setFont(Styles.FONT_PRICE);
        totalValue.setForeground(Styles.PRIMARY_BLUE);
        totalValue.setBackground(Styles.BG_WHITE);
        totalValue.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    }

    private void renderActionsRow(Composite card) {
        var row = new Composite(card, SWT.NONE);
        var rowGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        rowGd.verticalIndent = 24;
        row.setLayoutData(rowGd);
        row.setBackground(Styles.BG_WHITE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        row.setLayout(rowLayout);

        // "← Continuar comprando"
        var backBtn = createActionButton(row, ICON_ARROW_LEFT, "Continuar comprando");
        ((GridData) backBtn.getLayoutData()).grabExcessHorizontalSpace = true;
        backBtn.addListener(SWT.MouseUp, evt -> safeAction("onOpenProducts", () -> presenter.onOpenProducts()));

        // "Finalizar pedido" primary button
        var buyBtn = createPrimaryButton(row, ICON_CHECK2_SQUARE, "Finalizar pedido");
        buyBtn.addListener(SWT.MouseUp, evt -> safeAction("onBuy", () -> presenter.onBuy()));
    }

    private void renderErrorBanner(Composite card) {
        var banner = new Canvas(card, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var bannerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        bannerGd.heightHint = 40;
        bannerGd.verticalIndent = 16;
        banner.setLayoutData(bannerGd);
        banner.addPaintListener(ev -> {
            var gc = ev.gc;
            var area = banner.getClientArea();
            gc.setAntialias(SWT.ON);
            // White card background behind banner
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRectangle(0, 0, area.width, area.height);
            // Red-tinted background with rounded corners
            gc.setBackground(Styles.BG_ERROR);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 8, 8);
            // Red border
            gc.setForeground(Styles.BORDER_ERROR_BOX);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);
            // Icon
            gc.setForeground(Styles.FG_ERROR);
            gc.setFont(Styles.FONT_ICON);
            var iconSz = gc.textExtent(Styles.ICON_EXCLAMATION_CIRCLE);
            int iconX = 12;
            int iconY = (area.height - iconSz.y) / 2;
            gc.drawText(Styles.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);
            // Error text
            String msg = state.errorMessage != null ? state.errorMessage : "";
            int textX = iconX + iconSz.x + 10;
            var tl = new org.eclipse.swt.graphics.TextLayout(ev.display);
            tl.setText(msg);
            tl.setFont(Styles.FONT_BODY);
            tl.setWidth(area.width - textX - 12);
            gc.setForeground(Styles.FG_ERROR);
            int textY = (area.height - tl.getBounds().height) / 2;
            tl.draw(gc, textX, textY);
            // Resize height if needed
            int neededHeight = tl.getBounds().height + 24;
            tl.dispose();
            if (neededHeight > area.height) {
                bannerGd.heightHint = neededHeight;
                banner.getParent().layout(true, true);
            }
        });
    }

    // ========== BUTTON HELPERS (same as ProductViewSwt) ==========

    private Canvas createSmallIconButton(Composite parent, String icon) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.widthHint = 30;
        gd.heightHint = 30;
        btn.setLayoutData(gd);
        btn.setBackground(Styles.BG_WHITE);
        btn.setCursor(btn.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        final boolean[] hovered = {false};

        btn.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = 30, h = 30;
            if (hovered[0]) {
                gc.setBackground(BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
                gc.setForeground(Styles.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 6, 6);
            } else {
                gc.setBackground(Styles.BG_WHITE);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
            }
            gc.setFont(Styles.FONT_ICON);
            gc.setForeground(Styles.FG_TEXT_DARK);
            var ext = gc.textExtent(icon);
            gc.drawText(icon, (w - ext.x) / 2, (h - ext.y) / 2, true);
        });

        btn.addListener(SWT.MouseEnter, evt -> { hovered[0] = true; btn.redraw(); });
        btn.addListener(SWT.MouseExit, evt -> { hovered[0] = false; btn.redraw(); });

        return btn;
    }

    private Canvas createRemoveButton(Composite parent) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.widthHint = 30;
        gd.heightHint = 30;
        btn.setLayoutData(gd);
        btn.setBackground(Styles.BG_WHITE);
        btn.setCursor(btn.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        final boolean[] hovered = {false};

        btn.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = 30, h = 30;
            if (hovered[0]) {
                gc.setBackground(BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
                gc.setForeground(Styles.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 6, 6);
            } else {
                gc.setBackground(Styles.BG_WHITE);
                gc.fillRoundRectangle(0, 0, w, h, 6, 6);
            }
            gc.setFont(Styles.FONT_ICON_NAV); // larger icon (18pt)
            gc.setForeground(Styles.FG_PRICE); // red
            var ext = gc.textExtent(ICON_X);
            gc.drawText(ICON_X, (w - ext.x) / 2, (h - ext.y) / 2, true);
        });

        btn.addListener(SWT.MouseEnter, evt -> { hovered[0] = true; btn.redraw(); });
        btn.addListener(SWT.MouseExit, evt -> { hovered[0] = false; btn.redraw(); });

        return btn;
    }

    private Canvas createActionButton(Composite parent, String icon, String text) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        btn.setBackground(Styles.BG_WHITE);
        btn.setCursor(btn.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        // Compute width
        var tmpGc = new org.eclipse.swt.graphics.GC(btn);
        tmpGc.setFont(Styles.FONT_BODY);
        var textExt = tmpGc.textExtent(text);
        tmpGc.setFont(Styles.FONT_ICON);
        var iconExt = tmpGc.textExtent(icon);
        tmpGc.dispose();
        int btnW = iconExt.x + textExt.x + 24;
        int btnH = 36;

        var gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd.widthHint = btnW;
        gd.heightHint = btnH;
        btn.setLayoutData(gd);

        final boolean[] hovered = {false};

        btn.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = btn.getBounds().width;
            int h = btn.getBounds().height;

            if (hovered[0]) {
                gc.setBackground(BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
                gc.setForeground(Styles.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 8, 8);
            } else {
                gc.setBackground(Styles.BG_WHITE);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            }

            // Icon
            gc.setFont(Styles.FONT_ICON);
            gc.setForeground(Styles.FG_TEXT_DARK);
            var ie = gc.textExtent(icon);
            gc.drawText(icon, 10, (h - ie.y) / 2, true);

            // Text
            gc.setFont(Styles.FONT_BODY);
            gc.setForeground(Styles.FG_TEXT_DARK);
            gc.drawText(text, 10 + ie.x, (h - textExt.y) / 2, true);
        });

        btn.addListener(SWT.MouseEnter, evt -> { hovered[0] = true; btn.redraw(); });
        btn.addListener(SWT.MouseExit, evt -> { hovered[0] = false; btn.redraw(); });

        return btn;
    }

    private Canvas createPrimaryButton(Composite parent, String icon, String text) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        btn.setBackground(Styles.BG_WHITE);
        btn.setCursor(btn.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        // Compute width
        var tmpGc = new org.eclipse.swt.graphics.GC(btn);
        tmpGc.setFont(Styles.FONT_BODY_BOLD);
        var textExt = tmpGc.textExtent(text);
        tmpGc.setFont(Styles.FONT_ICON);
        var iconExt = tmpGc.textExtent(icon);
        tmpGc.dispose();
        int btnW = iconExt.x + textExt.x + 36;
        int btnH = 42;

        var gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        gd.widthHint = btnW;
        gd.heightHint = btnH;
        btn.setLayoutData(gd);

        final boolean[] hovered = {false};

        btn.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = btn.getBounds().width;
            int h = btn.getBounds().height;

            // Filled blue background
            gc.setBackground(hovered[0] ? new Color(0x0B, 0x5A, 0xBA) : Styles.PRIMARY_BLUE);
            gc.fillRoundRectangle(0, 0, w, h, 8, 8);

            // Icon
            gc.setFont(Styles.FONT_ICON);
            gc.setForeground(Styles.BG_WHITE);
            var ie = gc.textExtent(icon);
            int x = 16;
            gc.drawText(icon, x, (h - ie.y) / 2, true);

            // Text
            gc.setFont(Styles.FONT_BODY_BOLD);
            gc.setForeground(Styles.BG_WHITE);
            gc.drawText(text, x + ie.x + 6, (h - textExt.y) / 2, true);
        });

        btn.addListener(SWT.MouseEnter, evt -> { hovered[0] = true; btn.redraw(); });
        btn.addListener(SWT.MouseExit, evt -> { hovered[0] = false; btn.redraw(); });

        return btn;
    }
}
