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
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
            GluonDom.render((VBox) this.element, this::buildUI);
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

    private void buildUI(GluonDom dom, VBox root) {
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle(GluonStyles.PAGE_BG);

        // Header bar
        dom.hbox(headerBar -> {
            headerBar.setAlignment(Pos.CENTER_LEFT);
            headerBar.setSpacing(12);
            headerBar.setPadding(new Insets(10, 16, 10, 16));
            headerBar.setStyle(GluonStyles.HEADER_BAR);

            dom.button(backBtn -> {
                backBtn.setText("Voltar");
                backBtn.setGraphic(GluonIcons.create(GluonIcons.ARROW_BACK, 14, GluonColors.PRIMARY));
                backBtn.setStyle(GluonStyles.BACK_BUTTON);
                backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));
            });

            dom.hSpacer();

            dom.label(headerTitle -> {
                headerTitle.setText("Carrinho");
                headerTitle.setStyle(GluonStyles.PAGE_TITLE);
            });
        });

        // Empty cart state
        this.emptyPane = dom.vbox(empty -> {
            empty.setAlignment(Pos.CENTER);
            empty.setSpacing(8);
            empty.setPadding(new Insets(40, 0, 40, 0));
            VBox.setVgrow(empty, Priority.ALWAYS);

            dom.icon(GluonIcons.create(GluonIcons.SHOPPING_CART, 48, GluonColors.TEXT_PLACEHOLDER));

            dom.label(msg -> {
                msg.setText("Seu carrinho está vazio");
                msg.setStyle(GluonStyles.TEXT_SECONDARY_STYLE);
            });

            dom.label(hint -> {
                hint.setText("Vamos às compras!");
                hint.setStyle(GluonStyles.LINK_BOLD);
                hint.setOnMouseClicked(e -> safeAction("Go shopping", this.presenter::onOpenProducts));
            });
        });

        // Cart content
        this.contentPane = dom.vbox(content -> {
            content.setSpacing(10);
            content.setPadding(new Insets(12));
            VBox.setVgrow(content, Priority.ALWAYS);

            dom.scrollPane(sp -> {
                VBox.setVgrow(sp, Priority.ALWAYS);
                sp.setFitToWidth(true);
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

                var itemsBox = new VBox(6);
                sp.setContent(itemsBox);
                this.itemsSlot = this.newListSlot(itemsBox, this::newItemView, this::updateItem);
            });

            // Footer
            dom.hbox(footerRow -> {
                footerRow.setAlignment(Pos.CENTER);
                footerRow.setSpacing(8);
                footerRow.setPadding(new Insets(8, 0, 0, 0));

                dom.label(totalLabel -> {
                    totalLabel.setText("TOTAL:");
                    totalLabel.setStyle(GluonStyles.textBold(14, GluonColors.TEXT_DEFAULT));
                });

                dom.hSpacer();

                this.totalCostElm = dom.label(total -> {
                    total.setText(NumberFormat.getCurrencyInstance().format(computeTotalCost()));
                    total.setStyle(GluonStyles.PRICE_SMALL);
                });
                this.totalCostOldValue = computeTotalCost();
            });

            // Error
            this.errorElm = dom.label(err -> {
                err.setStyle(GluonStyles.ERROR_TEXT);
                err.setVisible(false);
                err.setManaged(false);
                err.setWrapText(true);
            });

            // Buy button
            dom.button(buyBtn -> {
                buyBtn.setText("FINALIZAR PEDIDO");
                buyBtn.setMaxWidth(Double.MAX_VALUE);
                buyBtn.setStyle(GluonStyles.BTN_SUCCESS_BLOCK);
                buyBtn.setOnAction(e -> safeAction("Buy", this.presenter::onBuy));
            });
        });
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
                GluonDom.render((HBox) this.element, this::buildUI);
                this.notRendered = false;
            }
            this.nameElm.setText(this.item.name != null ? this.item.name : "");
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(this.item.price));
            this.qtyElm.setText("x" + this.item.quantity);
        }

        private void buildUI(GluonDom dom, HBox row) {
            row.setSpacing(8);
            row.setPadding(new Insets(8));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(GluonStyles.CARD_ITEM);

            this.nameElm = dom.label(name -> {
                name.setStyle(GluonStyles.text(12, GluonColors.TEXT_DEFAULT));
                HBox.setHgrow(name, Priority.ALWAYS);
            });

            this.qtyElm = dom.label(qty -> {
                qty.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY));
            });

            this.priceElm = dom.label(price -> {
                price.setStyle(GluonStyles.text(12, GluonColors.PRIMARY));
            });

            dom.button(btn -> {
                btn.setGraphic(GluonIcons.create(GluonIcons.DELETE, 16, GluonColors.ERROR));
                btn.setStyle(GluonStyles.BTN_DANGER_INLINE);
                btn.setOnAction(e -> safeAction("Remove",
                        () -> this.presenter.onRemoveProduct(this.item.id)));
            });
        }
    }
}
