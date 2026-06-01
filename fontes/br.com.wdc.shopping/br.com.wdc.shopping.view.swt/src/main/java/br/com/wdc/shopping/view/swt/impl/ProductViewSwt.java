package br.com.wdc.shopping.view.swt.impl;

import java.text.NumberFormat;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.ProductImageCache;
import br.com.wdc.shopping.view.swt.util.Styles;

public class ProductViewSwt extends AbstractViewSwt<ProductPresenter> {

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    // Icons
    private static final String ICON_BAG_PLUS = "\uF176";
    private static final String ICON_DASH = "\uF2EA";
    private static final String ICON_PLUS = "\uF4FE";
    private static final String ICON_ARROW_LEFT = "\uF12F";

    // Fonts
    private static final Font FONT_TITLE;
    private static final Font FONT_DESC;
    private static final Font FONT_PRICE_LARGE;
    private static final Font FONT_QTY;
    private static final Font FONT_QTY_VALUE;

    // Colors
    private static final Color BG_PRICE_BADGE = new Color(0xE8, 0xF1, 0xFC);
    private static final Color BG_BTN_HOVER = new Color(0xEE, 0xEE, 0xEE);

    static {
        var display = Display.getDefault();
        var sysName = display.getSystemFont().getFontData()[0].getName();
        FONT_TITLE = new Font(display, new FontData(sysName, 20, SWT.BOLD));
        FONT_DESC = new Font(display, new FontData(sysName, 12, SWT.NORMAL));
        FONT_PRICE_LARGE = new Font(display, new FontData(sysName, 18, SWT.BOLD));
        FONT_QTY = new Font(display, new FontData(sysName, 11, SWT.NORMAL));
        FONT_QTY_VALUE = new Font(display, new FontData(sysName, 14, SWT.BOLD));
    }

    private final ProductViewState state;
    private boolean notRendered = true;
    private int quantity = 1;
    private Label qtyLabel;
    private Canvas errorBanner;
    private Label errorText;

    public ProductViewSwt(ProductPresenter presenter) {
        super("product", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.notRendered = false;
            render();
        }
        // Update error visibility
        if (errorBanner != null && !errorBanner.isDisposed()) {
            boolean showError = state.errorMessage != null && !state.errorMessage.isEmpty();
            ((GridData) errorBanner.getLayoutData()).exclude = !showError;
            errorBanner.setVisible(showError);
            if (showError && errorText != null) {
                errorText.setText(state.errorMessage);
            }
            element.layout(true, true);
        }
    }

    private void render() {
        var root = this.element;
        root.setBackground(Styles.BG_PAGE);

        var rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = 20;
        rootLayout.marginHeight = 20;
        rootLayout.verticalSpacing = 0;
        root.setLayout(rootLayout);

        var product = state.product;
        String name = product != null ? product.name : "";
        String description = product != null ? product.description : "";
        double price = product != null ? product.price : 0;

        // Title
        var titleLabel = new Label(root, SWT.NONE);
        titleLabel.setText(name);
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Styles.FG_TEXT_DARK);
        titleLabel.setBackground(Styles.BG_PAGE);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Blue divider
        var divider = new Canvas(root, SWT.NONE);
        var divGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        divGd.heightHint = 3;
        divGd.verticalIndent = 8;
        divider.setLayoutData(divGd);
        divider.setBackground(Styles.BG_PAGE);
        divider.addPaintListener(e -> {
            var gc = e.gc;
            gc.setBackground(Styles.PRIMARY_BLUE);
            gc.fillRectangle(0, 0, divider.getBounds().width, 3);
        });

        // Description card
        renderDescriptionCard(root, description);

        // Price + Image row
        renderPriceImageRow(root, product, price);

        // Actions row
        renderActionsRow(root);

        // Error banner (hidden by default)
        renderErrorBanner(root);

        root.layout(true, true);
    }

    private void renderDescriptionCard(Composite parent, String description) {
        var card = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var cardGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        cardGd.verticalIndent = 16;
        card.setLayoutData(cardGd);

        // Parse description as bullet points (HTML list items or newlines)
        var lines = parseDescription(description);

        card.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = card.getClientArea();

            // White rounded card
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 16, 16);

            // Border
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 16, 16);

            // Draw bullet text
            gc.setFont(FONT_DESC);
            gc.setForeground(Styles.FG_TEXT_DARK);
            int textX = 28;
            int textY = 24;
            int lineH = 26;

            for (var line : lines) {
                if (!line.isBlank()) {
                    gc.drawText("\u2022  " + line.trim(), textX, textY, true);
                    textY += lineH;
                }
            }
        });

        // Compute height based on line count
        int lineCount = Math.max(1, lines.length);
        cardGd.heightHint = 24 + lineCount * 26 + 20;
    }

    private String[] parseDescription(String desc) {
        if (desc == null || desc.isBlank()) return new String[]{"Sem descrição."};
        // Strip HTML tags, split by <li> or newlines
        desc = desc.replaceAll("</?ul>|</?ol>", "");
        desc = desc.replaceAll("<br\\s*/?>", "\n");
        if (desc.contains("<li>")) {
            var parts = desc.split("<li>");
            var result = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = parts[i].replaceAll("<[^>]+>", "").trim();
            }
            return result;
        }
        return desc.split("\n");
    }

    private void renderPriceImageRow(Composite parent, Object product, double price) {
        var row = new Composite(parent, SWT.NONE);
        var rowGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        rowGd.verticalIndent = 20;
        row.setLayoutData(rowGd);
        row.setBackground(Styles.BG_PAGE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 32;
        row.setLayout(rowLayout);

        // Left column: price + qty
        var leftCol = new Composite(row, SWT.NONE);
        leftCol.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        leftCol.setBackground(Styles.BG_PAGE);

        var leftLayout = new GridLayout(1, false);
        leftLayout.marginWidth = 0;
        leftLayout.marginHeight = 0;
        leftLayout.verticalSpacing = 12;
        leftCol.setLayout(leftLayout);

        // Price badge
        var priceBadge = new Canvas(leftCol, SWT.DOUBLE_BUFFERED);
        var badgeGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        badgeGd.heightHint = 48;
        badgeGd.widthHint = 160;
        priceBadge.setLayoutData(badgeGd);
        priceBadge.setBackground(Styles.BG_PAGE);

        String priceText = price > 0 ? PRICE_FORMAT.format(price) : "";
        priceBadge.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = priceBadge.getBounds().width;
            int h = priceBadge.getBounds().height;
            gc.setBackground(BG_PRICE_BADGE);
            gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            gc.setFont(FONT_PRICE_LARGE);
            gc.setForeground(Styles.PRIMARY_BLUE);
            var ext = gc.textExtent(priceText);
            gc.drawText(priceText, (w - ext.x) / 2, (h - ext.y) / 2, true);
        });

        // Quantity row
        var qtyRow = new Composite(leftCol, SWT.NONE);
        qtyRow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        qtyRow.setBackground(Styles.BG_PAGE);

        var qtyLayout = new GridLayout(4, false);
        qtyLayout.marginWidth = 0;
        qtyLayout.marginHeight = 0;
        qtyLayout.horizontalSpacing = 10;
        qtyRow.setLayout(qtyLayout);

        var qtyLbl = new Label(qtyRow, SWT.NONE);
        qtyLbl.setText("Qtd:");
        qtyLbl.setFont(FONT_QTY);
        qtyLbl.setForeground(Styles.FG_TEXT_SUBTLE);
        qtyLbl.setBackground(Styles.BG_PAGE);

        // Minus button
        var minusBtn = createSmallIconButton(qtyRow, ICON_DASH);
        minusBtn.addListener(SWT.MouseUp, evt -> {
            if (this.quantity > 1) {
                this.quantity--;
                qtyLabel.setText(String.valueOf(this.quantity));
                qtyLabel.getParent().layout(true);
            }
        });

        // Quantity value
        this.qtyLabel = new Label(qtyRow, SWT.CENTER);
        this.qtyLabel.setText("1");
        this.qtyLabel.setFont(FONT_QTY_VALUE);
        this.qtyLabel.setForeground(Styles.FG_TEXT_DARK);
        this.qtyLabel.setBackground(Styles.BG_PAGE);
        var qtyValGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        qtyValGd.widthHint = 28;
        this.qtyLabel.setLayoutData(qtyValGd);

        // Plus button
        var plusBtn = createSmallIconButton(qtyRow, ICON_PLUS);
        plusBtn.addListener(SWT.MouseUp, evt -> {
            this.quantity++;
            qtyLabel.setText(String.valueOf(this.quantity));
            qtyLabel.getParent().layout(true);
        });

        // Right column: image
        var imageBox = new Canvas(row, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var imgGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        imgGd.widthHint = 240;
        imgGd.heightHint = 240;
        imageBox.setLayoutData(imgGd);

        long productId = state.product != null ? state.product.id : -1;
        imageBox.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = imageBox.getBounds().width;
            int h = imageBox.getBounds().height;

            // Background gradient
            gc.setBackground(new Color(0xF8, 0xFA, 0xFC));
            gc.setForeground(new Color(0xEE, 0xF2, 0xF7));
            gc.fillGradientRectangle(0, 0, w, h, true);

            // Round clip
            gc.setBackground(new Color(0xEE, 0xF2, 0xF7));
            gc.fillRoundRectangle(0, 0, w, h, 12, 12);

            // Image
            var productImage = ProductImageCache.getInstance().getImage(imageBox.getDisplay(), productId);
            if (productImage != null && !productImage.isDisposed()) {
                var imgBounds = productImage.getBounds();
                double scale = Math.min((double) w / imgBounds.width, (double) h / imgBounds.height) * 0.85;
                int drawW = (int) (imgBounds.width * scale);
                int drawH = (int) (imgBounds.height * scale);
                int drawX = (w - drawW) / 2;
                int drawY = (h - drawH) / 2;
                gc.setInterpolation(SWT.HIGH);
                gc.drawImage(productImage, 0, 0, imgBounds.width, imgBounds.height, drawX, drawY, drawW, drawH);
            } else {
                gc.setFont(Styles.FONT_ICON_LARGE);
                gc.setForeground(Styles.FG_TEXT_SUBTLE);
                var icon = Styles.ICON_BAG_CHECK;
                var ext = gc.textExtent(icon);
                gc.drawText(icon, (w - ext.x) / 2, (h - ext.y) / 2, true);
            }
        });
    }

    private void renderActionsRow(Composite parent) {
        var row = new Composite(parent, SWT.NONE);
        var rowGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        rowGd.verticalIndent = 20;
        row.setLayoutData(rowGd);
        row.setBackground(Styles.BG_PAGE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 20;
        row.setLayout(rowLayout);

        // Back button (action button style)
        var backBtn = createActionButton(row, ICON_ARROW_LEFT, " Voltar");
        backBtn.addListener(SWT.MouseUp, evt -> {
            safeAction("product.onOpenProducts", () -> presenter.onOpenProducts());
        });

        // Add to Cart button (accent/primary style)
        var addCartBtn = createPrimaryButton(row, ICON_BAG_PLUS, "Adicionar ao Carrinho");
        addCartBtn.addListener(SWT.MouseUp, evt -> {
            safeAction("product.onAddToCart", () -> presenter.onAddToCart(this.quantity));
        });
    }

    private void renderErrorBanner(Composite parent) {
        errorBanner = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        var gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.verticalIndent = 12;
        gd.exclude = true;
        errorBanner.setLayoutData(gd);
        errorBanner.setVisible(false);
        errorBanner.setBackground(Styles.BG_PAGE);

        var bannerComposite = new Composite(errorBanner, SWT.NONE);
        bannerComposite.setBackground(new Color(0xFD, 0xED, 0xED));

        var bl = new GridLayout(2, false);
        bl.marginWidth = 12;
        bl.marginHeight = 10;
        bl.horizontalSpacing = 8;
        bannerComposite.setLayout(bl);

        var iconLbl = new Label(bannerComposite, SWT.NONE);
        iconLbl.setText("\u26A0");
        iconLbl.setFont(Styles.FONT_BODY);
        iconLbl.setForeground(new Color(0xC6, 0x28, 0x28));
        iconLbl.setBackground(new Color(0xFD, 0xED, 0xED));

        errorText = new Label(bannerComposite, SWT.WRAP);
        errorText.setText("");
        errorText.setFont(Styles.FONT_BODY);
        errorText.setForeground(new Color(0xC6, 0x28, 0x28));
        errorText.setBackground(new Color(0xFD, 0xED, 0xED));
        errorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        errorBanner.addListener(SWT.Resize, e -> {
            var area = errorBanner.getClientArea();
            bannerComposite.setBounds(0, 0, area.width, area.height);
        });
    }

    // -- Button helpers --

    private Canvas createSmallIconButton(Composite parent, String icon) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.widthHint = 30;
        gd.heightHint = 30;
        btn.setLayoutData(gd);
        btn.setBackground(Styles.BG_PAGE);
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
                gc.setBackground(Styles.BG_PAGE);
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

    private Canvas createActionButton(Composite parent, String icon, String text) {
        var btn = new Canvas(parent, SWT.DOUBLE_BUFFERED);
        btn.setBackground(Styles.BG_PAGE);
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

        var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
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
        btn.setBackground(Styles.BG_PAGE);
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

        var gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
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
