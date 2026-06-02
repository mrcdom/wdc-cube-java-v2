package br.com.wdc.shopping.view.swt.impl;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.ProductImageCache;
import br.com.wdc.shopping.view.swt.util.Styles;

/**
 * Products grid panel — scrollable grid of product cards.
 */
public class ProductsPanelViewSwt extends AbstractViewSwt<ProductsPanelPresenter> {

    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 195;
    private static final int CARD_IMAGE_HEIGHT = 130;
    private static final int CARD_PADDING = 12;
    private static final int GRID_GAP = 16;

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private ScrolledComposite scrolled;
    private Composite gridPanel;
    private int lastProductsHash;

    public ProductsPanelViewSwt(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
        this.element.setBackground(Styles.BG_PAGE);
        var layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        this.element.setLayout(layout);
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var products = this.state.products;
        int hash = products != null ? products.hashCode() : 0;
        if (hash != this.lastProductsHash) {
            this.lastProductsHash = hash;
            rebuildGrid(products);
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.scrolled = null;
        this.gridPanel = null;
        this.lastProductsHash = 0;
    }

    private void initialRender() {
        this.scrolled = new ScrolledComposite(this.element, SWT.V_SCROLL);
        this.scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        this.scrolled.setExpandHorizontal(true);
        this.scrolled.setExpandVertical(false);
        this.scrolled.setBackground(Styles.BG_PAGE);

        this.gridPanel = new Composite(this.scrolled, SWT.NONE);
        this.gridPanel.setBackground(Styles.BG_PAGE);
        this.scrolled.setContent(this.gridPanel);

        // Use RowLayout to wrap cards
        var rl = new RowLayout(SWT.HORIZONTAL);
        rl.wrap = true;
        rl.spacing = GRID_GAP;
        rl.marginLeft = GRID_GAP;
        rl.marginTop = GRID_GAP;
        rl.marginRight = GRID_GAP;
        rl.marginBottom = GRID_GAP;
        this.gridPanel.setLayout(rl);
    }

    private void rebuildGrid(List<ProductInfo> products) {
        if (this.gridPanel == null) return;

        // Dispose old cards
        for (var child : this.gridPanel.getChildren()) {
            child.dispose();
        }

        if (products != null) {
            for (var product : products) {
                createProductCard(product);
            }
        }

        // Recompute scrolled size
        var size = this.gridPanel.computeSize(
                this.scrolled.getClientArea().width > 0 ? this.scrolled.getClientArea().width : SWT.DEFAULT,
                SWT.DEFAULT);
        this.gridPanel.setSize(size);

        // Listen for resize to reflow
        this.scrolled.addListener(SWT.Resize, _e -> {
            var s = this.gridPanel.computeSize(this.scrolled.getClientArea().width, SWT.DEFAULT);
            this.gridPanel.setSize(s);
        });
    }

    private void createProductCard(ProductInfo product) {
        var card = new Canvas(this.gridPanel, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        card.setCursor(card.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        card.setData("productId", product.id);

        card.addPaintListener(e -> {
            var gc = e.gc;
            var area = card.getClientArea();
            gc.setAntialias(SWT.ON);

            // Card background with rounded corners
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRoundRectangle(0, 0, area.width, area.height, Styles.CARD_RADIUS, Styles.CARD_RADIUS);

            // Border
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, Styles.CARD_RADIUS, Styles.CARD_RADIUS);

            // Image area gradient
            gc.setBackground(Styles.BG_PAGE);
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.fillGradientRectangle(1, 1, area.width - 2, CARD_IMAGE_HEIGHT, true);

            // Product image or fallback icon
            var productImage = ProductImageCache.getInstance().getImage(card.getDisplay(), product.id);
            if (productImage != null && !productImage.isDisposed()) {
                var imgBounds = productImage.getBounds();
                // Scale image to fit within the image area, maintaining aspect ratio
                int availW = area.width - 2;
                int availH = CARD_IMAGE_HEIGHT;
                double scale = Math.min((double) availW / imgBounds.width, (double) availH / imgBounds.height);
                int drawW = (int) (imgBounds.width * scale);
                int drawH = (int) (imgBounds.height * scale);
                int drawX = (area.width - drawW) / 2;
                int drawY = (CARD_IMAGE_HEIGHT - drawH) / 2;
                gc.setInterpolation(SWT.HIGH);
                gc.drawImage(productImage, 0, 0, imgBounds.width, imgBounds.height, drawX, drawY, drawW, drawH);
            } else {
                // Fallback: icon placeholder
                gc.setFont(Styles.FONT_ICON_LARGE);
                gc.setForeground(Styles.FG_TEXT_SUBTLE);
                var iconText = Styles.ICON_BAG_CHECK;
                Point iconSz = gc.textExtent(iconText);
                gc.drawText(iconText, (area.width - iconSz.x) / 2, (CARD_IMAGE_HEIGHT - iconSz.y) / 2, true);
            }

            // Product name
            int textY = CARD_IMAGE_HEIGHT + CARD_PADDING;
            gc.setFont(Styles.FONT_PRODUCT_NAME);
            gc.setForeground(Styles.FG_TEXT_DARK);
            String name = product.name != null ? product.name : "";
            if (name.length() > 22) name = name.substring(0, 20) + "...";
            gc.drawText(name, CARD_PADDING, textY, true);

            // Price
            gc.setFont(Styles.FONT_PRICE);
            gc.setForeground(Styles.PRIMARY_BLUE);
            String price = PRICE_FORMAT.format(product.price);
            gc.drawText(price, CARD_PADDING, textY + 24, true);
        });

        card.addListener(SWT.MouseDown, _e -> {
            var pid = (Long) card.getData("productId");
            safeAction("openProduct", () -> this.presenter.onOpenProduct(pid));
        });

        // Set size via RowData
        var rd = new org.eclipse.swt.layout.RowData(CARD_WIDTH, CARD_HEIGHT);
        card.setLayoutData(rd);
    }
}
