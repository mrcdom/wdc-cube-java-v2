package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ProductsPanelViewGluon extends AbstractViewGluon<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductCardView> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductCardView>> contentSlot;

    public ProductsPanelViewGluon(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingGluonApplication) presenter.app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((VBox) this.element, this::buildUI);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void buildUI(GluonDom dom, VBox root) {
        root.setPadding(new Insets(8));
        root.setSpacing(8);

        dom.label(caption -> {
            caption.setText("PRODUTOS");
            caption.setStyle(GluonStyles.SECTION_TITLE);
        });

        dom.scrollFlowPane((sp, flowPane) -> {
            VBox.setVgrow(sp, Priority.ALWAYS);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

            flowPane.setHgap(12);
            flowPane.setVgap(12);
            flowPane.setPadding(new Insets(8));

            this.contentSlot = this.newListSlot(flowPane, this::newItemView, this::updateItem);
        });
    }

    private ProductCardView newItemView() {
        return new ProductCardView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductCardView itemView, ProductInfo state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class: ProductCardView ----

    public static class ProductCardView extends AbstractViewGluon<ProductsPanelPresenter> {

        /** Normal card style (cardShadowSm equivalent) */
        private static final String CARD_NORMAL = GluonStyles.CARD_SMALL;
        /** Hovered card style (cardShadowLg + border — matches Flutter HoverCard) */
        private static final String CARD_HOVERED = "-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: " + GluonColors.BORDER + "; -fx-border-radius: 12; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.16), 12, 0, 0, 8);";

        private ProductInfo product;
        private String oldProductName;
        private String oldProductPrice;
        private boolean notRendered = true;
        private ImageView imageElm;
        private String imageOldValue;
        private Label nameElm;
        private Label priceElm;

        ProductCardView(ShoppingGluonApplication app, ProductsPanelPresenter presenter, int idx) {
            super("product-card-" + idx, app, presenter, new VBox());
            this.product = new ProductInfo();
        }

        void setState(ProductInfo product) {
            this.product = product;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                GluonDom.render((VBox) this.element, this::buildUI);
                this.notRendered = false;
            }

            if (this.product.image != null && !this.product.image.equals(this.imageOldValue)) {
                var img = ResourceCatalog.getImage(this.product.image);
                this.imageElm.setImage(img);
                this.imageOldValue = this.product.image;
            }

            var newProductName = this.product.name != null ? this.product.name : "";
            if (!Objects.equals(oldProductName, newProductName)) {
                this.nameElm.setText(newProductName);
                this.oldProductName = newProductName;
            }

            var newProductPrice = this.product.price > 0
                    ? NumberFormat.getCurrencyInstance().format(this.product.price)
                    : "";
            if (!Objects.equals(oldProductPrice, newProductPrice)) {
                this.priceElm.setText(newProductPrice);
                this.oldProductPrice = newProductPrice;
            }
        }

        private void buildUI(GluonDom dom, VBox card) {
            card.setPrefWidth(172);
            card.setStyle(CARD_NORMAL);
            card.setOnMouseClicked(
                    e -> safeAction("Open product", () -> this.presenter.onOpenProduct(this.product.id)));

            // Hover effect — matches Flutter HoverCard: translateY(-3px) + cardShadowLg (200ms easeOut)
            var hoverTransition = new TranslateTransition(Duration.millis(200), card);
            card.setOnMouseEntered(e -> {
                hoverTransition.stop();
                hoverTransition.setToY(-3);
                hoverTransition.play();
                card.setStyle(CARD_HOVERED);
            });
            card.setOnMouseExited(e -> {
                hoverTransition.stop();
                hoverTransition.setToY(0);
                hoverTransition.play();
                card.setStyle(CARD_NORMAL);
            });
            // Image area with gradient background
            dom.stackPane(imageArea -> {
                imageArea.setStyle(GluonStyles.IMAGE_BG);
                imageArea.setPrefHeight(128);
                imageArea.setMinHeight(128);
                imageArea.setMaxWidth(Double.MAX_VALUE);

                this.imageElm = dom.imageView(img -> {
                    img.setFitWidth(90);
                    img.setFitHeight(90);
                    img.setPreserveRatio(true);
                });
            });

            // Info area
            dom.vbox(infoArea -> {
                infoArea.setSpacing(3);
                infoArea.setPadding(new Insets(10, 14, 12, 14));
                infoArea.setStyle("-fx-background-color: white;");

                this.nameElm = dom.label(name -> {
                    name.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_DEFAULT));
                    name.setWrapText(true);
                    name.setMaxWidth(158);
                });

                this.priceElm = dom.label(price -> {
                    price.setStyle(GluonStyles.textBold(13, GluonColors.PRIMARY));
                });
            });
        }
    }
}
