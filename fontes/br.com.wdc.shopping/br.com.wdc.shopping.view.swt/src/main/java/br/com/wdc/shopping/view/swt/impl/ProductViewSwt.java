package br.com.wdc.shopping.view.swt.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.components.AccentLine;
import br.com.wdc.shopping.view.swt.components.ActionButton;
import br.com.wdc.shopping.view.swt.components.ErrorBanner;
import br.com.wdc.shopping.view.swt.components.IconButton;
import br.com.wdc.shopping.view.swt.components.PrimaryButton;
import br.com.wdc.shopping.view.swt.components.ScrolledPage;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.ProductImageCache;

public class ProductViewSwt extends AbstractViewSwt<ProductPresenter> {

    private final ProductViewState state;
    private boolean notRendered = true;
    private int quantity = 1;
    private Label qtyLabel;
    private ErrorBanner errorBanner;

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
            if (showError) {
                errorBanner.setMessage(state.errorMessage);
            }
            if (showError != errorBanner.isShown()) {
                errorBanner.setShown(showError);
                // Recompute scroll size
                var content = errorBanner.getParent();
                content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        }
    }

    private void render() {
        var page = new ScrolledPage(this.element, 20, 20, 0);
        var content = page.getContent();

        var product = state.product;
        String name = product != null ? product.name : "";
        String description = product != null ? product.description : "";
        double price = product != null ? product.price : 0;

        // Title
        var titleLabel = new Label(content, SWT.NONE);
        titleLabel.setText(name);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.FG_TEXT_DARK);
        titleLabel.setBackground(Theme.BG_PAGE);
        titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Blue divider
        new AccentLine(content, 3, 8);

        // Description card
        renderDescriptionCard(content, description);

        // Price + Image row
        renderPriceImageRow(content, price);

        // Actions row
        renderActionsRow(content);

        // Error banner (hidden by default)
        errorBanner = new ErrorBanner(content, 12, false);

        page.complete();
    }

    private void renderDescriptionCard(Composite parent, String description) {
        var card = new Canvas(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var cardGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        cardGd.verticalIndent = 16;
        card.setLayoutData(cardGd);

        var lines = parseDescription(description);

        card.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = card.getClientArea();

            gc.setBackground(Theme.BG_WHITE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 16, 16);

            gc.setForeground(Theme.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 16, 16);

            gc.setFont(Theme.FONT_SUBTITLE);
            gc.setForeground(Theme.FG_TEXT_DARK);
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

        int lineCount = Math.max(1, lines.length);
        cardGd.heightHint = 24 + lineCount * 26 + 20;
    }

    private String[] parseDescription(String desc) {
        if (desc == null || desc.isBlank()) return new String[]{"Sem descrição."};
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

    private void renderPriceImageRow(Composite parent, double price) {
        var row = new Composite(parent, SWT.NONE);
        var rowGd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        rowGd.verticalIndent = 20;
        row.setLayoutData(rowGd);
        row.setBackground(Theme.BG_PAGE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 32;
        row.setLayout(rowLayout);

        // Left column: price + qty
        var leftCol = new Composite(row, SWT.NONE);
        leftCol.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        leftCol.setBackground(Theme.BG_PAGE);

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
        priceBadge.setBackground(Theme.BG_PAGE);

        String priceText = price > 0 ? Theme.formatPrice(price) : "";
        priceBadge.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = priceBadge.getBounds().width;
            int h = priceBadge.getBounds().height;
            gc.setBackground(Theme.BG_ICON_BOX);
            gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            gc.setFont(Theme.FONT_PRICE_LARGE);
            gc.setForeground(Theme.PRIMARY_BLUE);
            var ext = gc.textExtent(priceText);
            gc.drawText(priceText, (w - ext.x) / 2, (h - ext.y) / 2, true);
        });

        // Quantity row
        var qtyRow = new Composite(leftCol, SWT.NONE);
        qtyRow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        qtyRow.setBackground(Theme.BG_PAGE);

        var qtyLayout = new GridLayout(4, false);
        qtyLayout.marginWidth = 0;
        qtyLayout.marginHeight = 0;
        qtyLayout.horizontalSpacing = 10;
        qtyRow.setLayout(qtyLayout);

        var qtyLbl = new Label(qtyRow, SWT.NONE);
        qtyLbl.setText("Qtd:");
        qtyLbl.setFont(Theme.FONT_QTY);
        qtyLbl.setForeground(Theme.FG_TEXT_SUBTLE);
        qtyLbl.setBackground(Theme.BG_PAGE);

        // Minus button
        var minusBtn = new IconButton(qtyRow, Theme.ICON_DASH, Theme.BG_PAGE);
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
        this.qtyLabel.setFont(Theme.FONT_QTY_VALUE);
        this.qtyLabel.setForeground(Theme.FG_TEXT_DARK);
        this.qtyLabel.setBackground(Theme.BG_PAGE);
        var qtyValGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        qtyValGd.widthHint = 28;
        this.qtyLabel.setLayoutData(qtyValGd);

        // Plus button
        var plusBtn = new IconButton(qtyRow, Theme.ICON_PLUS, Theme.BG_PAGE);
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

            gc.setBackground(Theme.BG_IMAGE_PLACEHOLDER);
            gc.fillRoundRectangle(0, 0, w, h, 12, 12);

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
                gc.setFont(Theme.FONT_ICON_LARGE);
                gc.setForeground(Theme.FG_TEXT_SUBTLE);
                var icon = Theme.ICON_BAG_CHECK;
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
        row.setBackground(Theme.BG_PAGE);

        var rowLayout = new GridLayout(2, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.horizontalSpacing = 20;
        row.setLayout(rowLayout);

        // Back button
        var backBtn = new ActionButton(row, Theme.ICON_ARROW_LEFT, "Voltar", Theme.BG_PAGE);
        var backGd = (GridData) backBtn.getLayoutData();
        backGd.horizontalAlignment = SWT.CENTER;
        backBtn.addListener(SWT.MouseUp, evt -> safeAction("product.onOpenProducts", presenter::onOpenProducts));

        // Add to Cart button
        var addCartBtn = new PrimaryButton(row, Theme.ICON_BAG_PLUS, "Adicionar ao Carrinho", Theme.BG_PAGE);
        var addGd = (GridData) addCartBtn.getLayoutData();
        addGd.horizontalAlignment = SWT.CENTER;
        addCartBtn.addListener(SWT.MouseUp, evt -> {
            safeAction("product.onAddToCart", () -> presenter.onAddToCart(this.quantity));
        });
    }
}
