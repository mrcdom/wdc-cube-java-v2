package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProductsPanelViewGluon extends AbstractViewGluon<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private boolean notRendered = true;
    private int itemIdx;
    private List<ProductCardView> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductCardView>> contentSlot;

    public ProductsPanelViewGluon(ShoppingGluonApplication app, ProductsPanelPresenter presenter) {
        super("products-panel", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            buildUI();
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void buildUI() {
        var root = (VBox) this.element;
        root.setPadding(new Insets(8));
        root.setSpacing(8);

        var caption = new Label("PRODUTOS");
        caption.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #333;");

        var flowPane = new FlowPane(8, 8);
        flowPane.setPadding(new Insets(4));
        this.contentSlot = this.newListSlot(flowPane, this::newItemView, this::updateItem);

        var scrollPane = new ScrollPane(flowPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.getChildren().addAll(caption, scrollPane);
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

        private ProductInfo product;
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
                buildUI();
                this.notRendered = false;
            }
            if (this.product.image != null && !this.product.image.equals(this.imageOldValue)) {
                var img = ResourceCatalog.getImage(this.product.image);
                this.imageElm.setImage(img);
                this.imageOldValue = this.product.image;
            }
            this.nameElm.setText(this.product.name != null ? this.product.name : "");
            this.priceElm.setText(this.product.price > 0
                    ? NumberFormat.getCurrencyInstance().format(this.product.price) : "");
        }

        private void buildUI() {
            var card = (VBox) this.element;
            card.setPrefWidth(140);
            card.setPadding(new Insets(8));
            card.setSpacing(4);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 6; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1); -fx-cursor: hand;");
            card.setOnMouseClicked(e -> safeAction("Open product",
                    () -> this.presenter.onOpenProduct(this.product.id)));

            this.imageElm = new ImageView();
            this.imageElm.setFitWidth(100);
            this.imageElm.setFitHeight(100);
            this.imageElm.setPreserveRatio(true);

            this.nameElm = new Label();
            this.nameElm.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #333;");
            this.nameElm.setWrapText(true);
            this.nameElm.setMaxWidth(130);

            this.priceElm = new Label();
            this.priceElm.setStyle("-fx-font-size: 11; -fx-text-fill: #1976D2;");

            card.getChildren().addAll(this.imageElm, this.nameElm, this.priceElm);
        }
    }
}
