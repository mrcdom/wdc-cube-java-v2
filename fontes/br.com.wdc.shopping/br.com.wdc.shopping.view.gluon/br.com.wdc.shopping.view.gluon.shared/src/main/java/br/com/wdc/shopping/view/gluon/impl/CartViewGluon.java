package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class CartViewGluon extends AbstractViewGluon<CartPresenter> {

    private final CartViewState state;

    private boolean notRendered = true;
    private List<CartItemView> cartItemViewList = new ArrayList<>();
    private BiConsumer<List<CartItem>, List<CartItemView>> itemsSlot;
    private int itemIdx;
    private Label totalCostElm;
    private double totalCostOldValue;
    private Label errorElm;
    private VBox emptyPane;
    private VBox contentPane;

    public CartViewGluon(ShoppingGluonApplication app, CartPresenter presenter) {
        super("cart", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            buildUI();
            this.notRendered = false;
        }

        this.itemsSlot.accept(this.state.items, this.cartItemViewList);

        var empty = this.state.items == null || this.state.items.isEmpty();
        this.emptyPane.setVisible(empty);
        this.emptyPane.setManaged(empty);
        this.contentPane.setVisible(!empty);
        this.contentPane.setManaged(!empty);

        var totalCostNewValue = computeTotalCost();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setText(NumberFormat.getCurrencyInstance().format(totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
        if (!Objects.equals(this.errorElm.getText(), newErrorMessage)) {
            this.errorElm.setText(newErrorMessage);
        }
        if (this.errorElm.isVisible() != newErrorDisplay) {
            this.errorElm.setVisible(newErrorDisplay);
            this.errorElm.setManaged(newErrorDisplay);
        }
    }

    private void buildUI() {
        var root = (VBox) this.element;
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Header bar (consistent with Product and Receipt)
        var backBtn = new Button("← Voltar");
        backBtn.setStyle("-fx-font-size: 13; -fx-background-color: transparent; -fx-text-fill: #1976D2; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));

        var headerTitle = new Label("Carrinho");
        headerTitle.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #333;");

        var headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        var headerBar = new HBox(12, backBtn, headerSpacer, headerTitle);
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(10, 16, 10, 16));
        headerBar.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");

        // Empty cart state
        var emptyIcon = new Label("\uD83D\uDED2");
        emptyIcon.setStyle("-fx-font-size: 48;");
        var emptyMsg = new Label("Seu carrinho está vazio");
        emptyMsg.setStyle("-fx-font-size: 14; -fx-text-fill: #666;");
        var emptyHint = new Label("Vamos às compras!");
        emptyHint.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1976D2; -fx-cursor: hand; -fx-underline: true;");
        emptyHint.setOnMouseClicked(e -> safeAction("Go shopping", this.presenter::onOpenProducts));
        this.emptyPane = new VBox(8, emptyIcon, emptyMsg, emptyHint);
        this.emptyPane.setAlignment(Pos.CENTER);
        this.emptyPane.setPadding(new Insets(40, 0, 40, 0));
        VBox.setVgrow(this.emptyPane, Priority.ALWAYS);

        // Cart content
        var itemsBox = new VBox(6);
        this.itemsSlot = this.newListSlot(itemsBox, this::newItemView, this::updateItem);

        var scroll = new ScrollPane(itemsBox);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        var totalLabel = new Label("TOTAL:");
        totalLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        this.totalCostElm = new Label(NumberFormat.getCurrencyInstance().format(computeTotalCost()));
        this.totalCostElm.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        this.totalCostOldValue = computeTotalCost();

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        var footerRow = new HBox(8, totalLabel, spacer, this.totalCostElm);
        footerRow.setAlignment(Pos.CENTER);
        footerRow.setPadding(new Insets(8, 0, 0, 0));

        // Error
        this.errorElm = new Label();
        this.errorElm.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 12;");
        this.errorElm.setVisible(false);
        this.errorElm.setManaged(false);
        this.errorElm.setWrapText(true);

        // Buy button
        var buyBtn = new Button("FINALIZAR PEDIDO");
        buyBtn.setMaxWidth(Double.MAX_VALUE);
        buyBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 4;");
        buyBtn.setOnAction(e -> safeAction("Buy", this.presenter::onBuy));

        this.contentPane = new VBox(10, scroll, footerRow, this.errorElm, buyBtn);
        this.contentPane.setPadding(new Insets(12));
        VBox.setVgrow(this.contentPane, Priority.ALWAYS);

        root.getChildren().addAll(headerBar, this.emptyPane, this.contentPane);
    }

    private double computeTotalCost() {
        if (this.state.items == null) return 0;
        return this.state.items.stream().mapToDouble(v -> v.price * v.quantity).sum();
    }

    private CartItemView newItemView() {
        return new CartItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(CartItemView itemView, CartItem state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class ----

    public static class CartItemView extends AbstractViewGluon<CartPresenter> {

        private CartItem item;
        private boolean notRendered = true;
        private Label nameElm;
        private Label priceElm;
        private Label qtyElm;
        private Button removeBtn;

        CartItemView(ShoppingGluonApplication app, CartPresenter presenter, int idx) {
            super("cart-item-" + idx, app, presenter, new HBox());
            this.item = new CartItem();
        }

        void setState(CartItem item) {
            this.item = item;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                buildUI();
                this.notRendered = false;
            }
            this.nameElm.setText(this.item.name != null ? this.item.name : "");
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.item.price));
            this.qtyElm.setText("x" + this.item.quantity);
        }

        private void buildUI() {
            var row = (HBox) this.element;
            row.setSpacing(8);
            row.setPadding(new Insets(8));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: white; -fx-background-radius: 4; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 2, 0, 0, 1);");

            this.nameElm = new Label();
            this.nameElm.setStyle("-fx-font-size: 12; -fx-text-fill: #333;");
            HBox.setHgrow(this.nameElm, Priority.ALWAYS);

            this.priceElm = new Label();
            this.priceElm.setStyle("-fx-font-size: 12; -fx-text-fill: #1976D2;");

            this.qtyElm = new Label();
            this.qtyElm.setStyle("-fx-font-size: 12; -fx-text-fill: #666;");

            this.removeBtn = new Button("🗑");
            this.removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #d32f2f; " +
                    "-fx-font-size: 14; -fx-padding: 2 6; -fx-cursor: hand;");
            this.removeBtn.setOnAction(e -> safeAction("Remove",
                    () -> this.presenter.onRemoveProduct(this.item.id)));

            row.getChildren().addAll(this.nameElm, this.qtyElm, this.priceElm, this.removeBtn);
        }
    }
}
