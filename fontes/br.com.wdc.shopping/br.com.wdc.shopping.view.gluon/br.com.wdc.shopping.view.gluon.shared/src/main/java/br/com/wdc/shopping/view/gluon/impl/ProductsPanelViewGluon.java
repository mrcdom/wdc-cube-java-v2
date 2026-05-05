package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
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

            flowPane.setHgap(8);
            flowPane.setVgap(8);
            flowPane.setPadding(new Insets(4));

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
            card.setPrefWidth(140);
            card.setPadding(new Insets(8));
            card.setSpacing(4);
            card.setAlignment(Pos.CENTER);
            card.setStyle(GluonStyles.CARD_SMALL);
            card.setOnMouseClicked(
                    e -> safeAction("Open product", () -> this.presenter.onOpenProduct(this.product.id)));

            this.imageElm = dom.imageView(img -> {
                img.setFitWidth(100);
                img.setFitHeight(100);
                img.setPreserveRatio(true);
            });

            this.nameElm = dom.label(name -> {
                name.setStyle(GluonStyles.textBold(12, GluonColors.TEXT_DEFAULT));
                name.setWrapText(true);
                name.setMaxWidth(130);
            });

            this.priceElm = dom.label(price -> {
                price.setStyle(GluonStyles.text(11, GluonColors.PRIMARY));
            });
        }
    }
}
