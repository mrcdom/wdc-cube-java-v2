package br.com.wdc.shopping.view.swt.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.components.ErrorBanner;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.ProductImageCache;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

public class ProductViewSwt extends AbstractViewSwt<ProductPresenter> {

    private final ProductViewState state;
    private boolean notRendered = true;
    private int quantity = 1;
    private Label qtyLabel;
    private ErrorBanner errorBanner;

    public ProductViewSwt(ProductPresenter presenter) {
        super("product", (ShoppingSwtApplication) presenter.app, presenter);
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
        var product = state.product;
        String name = product != null ? product.name : "";
        String description = product != null ? product.description : "";
        double price = product != null ? product.price : 0;
        long productId = product != null ? product.id : -1;

        SwtDom.render(this.element, (dom, _root) -> {
            dom.scrolledPage(page -> {
                // Title
                page.label(lbl -> {
                    lbl.setText(name);
                    lbl.setFont(Theme.FONT_TITLE);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_PAGE);
                    lbl.setLayoutData(gdFillH(new GridData()));
                });

                // Blue divider
                page.accentLine(3, 8);

                // Description card
                var lines = parseDescription(description);
                page.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, card -> {
                    var cardGd = new GridData();
                    gdFillH(cardGd);
                    gdIndent(cardGd, 0, 16);
                    int lineCount = Math.max(1, lines.length);
                    cardGd.heightHint = 24 + lineCount * 26 + 20;
                    card.setLayoutData(cardGd);
                    card.addPaintListener(e -> paintDescriptionCard(e.gc, card.getClientArea(), lines));
                });

                // Price + Image row
                page.row(2, row -> {
                    var rowGd = new GridData();
                    gdCenter(rowGd);
                    gdGrabH(rowGd);
                    gdIndent(rowGd, 0, 20);
                    row.setLayoutData(rowGd);
                    row.setBackground(Theme.BG_PAGE);
                    var rowLayout = (GridLayout) row.getLayout();
                    rowLayout.horizontalSpacing = 32;

                    // Left column: price + qty
                    page.col(leftCol -> {
                        leftCol.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
                        leftCol.setBackground(Theme.BG_PAGE);
                        var leftLayout = (GridLayout) leftCol.getLayout();
                        leftLayout.verticalSpacing = 12;

                        // Price badge
                        String priceText = price > 0 ? Theme.formatPrice(price) : "";
                        page.canvas(SWT.DOUBLE_BUFFERED, priceBadge -> {
                            var badgeGd = gdCenter(new GridData());
                            badgeGd.widthHint = 160;
                            badgeGd.heightHint = 48;
                            priceBadge.setLayoutData(badgeGd);
                            priceBadge.setBackground(Theme.BG_PAGE);
                            priceBadge.addPaintListener(e -> paintPriceBadge(e.gc, priceBadge.getBounds(), priceText));
                        });

                        // Quantity row
                        page.row(4, qtyRow -> {
                            qtyRow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
                            qtyRow.setBackground(Theme.BG_PAGE);
                            var qtyLayout = (GridLayout) qtyRow.getLayout();
                            qtyLayout.horizontalSpacing = 10;

                            page.label(lbl -> {
                                lbl.setText("Qtd:");
                                lbl.setFont(Theme.FONT_QTY);
                                lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                                lbl.setBackground(Theme.BG_PAGE);
                            });

                            page.iconButton(Theme.ICON_DASH, Theme.BG_PAGE, btn -> {
                                btn.addListener(SWT.MouseUp, evt -> {
                                    if (this.quantity > 1) {
                                        this.quantity--;
                                        qtyLabel.setText(String.valueOf(this.quantity));
                                        qtyLabel.getParent().layout(true);
                                    }
                                });
                            });

                            this.qtyLabel = page.label(SWT.CENTER, lbl -> {
                                lbl.setText("1");
                                lbl.setFont(Theme.FONT_QTY_VALUE);
                                lbl.setForeground(Theme.FG_TEXT_DARK);
                                lbl.setBackground(Theme.BG_PAGE);
                                var qtyGd = new GridData();
                                gdCenter(qtyGd);
                                gdWidth(qtyGd, 28);
                                lbl.setLayoutData(qtyGd);
                            });

                            page.iconButton(Theme.ICON_PLUS, Theme.BG_PAGE, btn -> {
                                btn.addListener(SWT.MouseUp, evt -> {
                                    this.quantity++;
                                    qtyLabel.setText(String.valueOf(this.quantity));
                                    qtyLabel.getParent().layout(true);
                                });
                            });
                        });
                    });

                    // Right column: image
                    page.canvas(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, imageBox -> {
                        var imgGd = gdCenter(new GridData());
                        imgGd.widthHint = 240;
                        imgGd.heightHint = 240;
                        imageBox.setLayoutData(imgGd);
                        imageBox.addPaintListener(e -> paintProductImage(e.gc, imageBox, productId));
                    });
                });

                // Actions row
                page.row(2, actionsRow -> {
                    var actionsGd = new GridData();
                    gdCenter(actionsGd);
                    gdGrabH(actionsGd);
                    gdIndent(actionsGd, 0, 20);
                    actionsRow.setLayoutData(actionsGd);
                    actionsRow.setBackground(Theme.BG_PAGE);
                    var actionsLayout = (GridLayout) actionsRow.getLayout();
                    actionsLayout.horizontalSpacing = 20;

                    page.actionButton(Theme.ICON_ARROW_LEFT, "Voltar", Theme.BG_PAGE, btn -> {
                        var gd = (GridData) btn.getLayoutData();
                        gd.horizontalAlignment = SWT.CENTER;
                        btn.addListener(SWT.MouseUp, evt -> safeAction("product.onOpenProducts", presenter::onOpenProducts));
                    });

                    page.primaryButton(Theme.ICON_BAG_PLUS, "Adicionar ao Carrinho", Theme.BG_PAGE, btn -> {
                        var gd = (GridData) btn.getLayoutData();
                        gd.horizontalAlignment = SWT.CENTER;
                        btn.addListener(SWT.MouseUp, evt -> {
                            safeAction("product.onAddToCart", () -> presenter.onAddToCart(this.quantity));
                        });
                    });
                });

                // Error banner (hidden by default)
                this.errorBanner = page.errorBanner(12, false, eb -> {});
            });
        });
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

    // ========== SURFACES ==========

    private void paintDescriptionCard(GC gc, Rectangle area, String[] lines) {
        Surface.drawCard(gc, area, 16);

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
    }

    private void paintPriceBadge(GC gc, Rectangle bounds, String priceText) {
        gc.setAntialias(SWT.ON);
        gc.setBackground(Theme.BG_ICON_BOX);
        gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 8, 8);
        gc.setFont(Theme.FONT_PRICE_LARGE);
        gc.setForeground(Theme.PRIMARY_BLUE);
        var ext = gc.textExtent(priceText);
        gc.drawText(priceText, (bounds.width - ext.x) / 2, (bounds.height - ext.y) / 2, true);
    }

    private void paintProductImage(GC gc, Canvas imageBox, long productId) {
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
    }
}
