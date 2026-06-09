package br.com.wdc.shopping.view.gluon.impl;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
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

    public CartViewGluon(CartPresenter presenter) {
        super("cart", (ShoppingGluonApplication) presenter.app, presenter, new VBox());
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

        // View header with icon + title + subtitle (like Flutter ViewHeader)
        dom.hbox(viewHeader -> {
            viewHeader.setAlignment(Pos.CENTER_LEFT);
            viewHeader.setSpacing(12);
            viewHeader.setPadding(new Insets(20, 20, 16, 20));

            dom.stackPane(iconBox -> {
                iconBox.setStyle(GluonStyles.VIEW_HEADER_ICON_BOX);
                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 22, GluonColors.PRIMARY));
            });

            dom.vbox(titleBox -> {
                titleBox.setSpacing(2);
                dom.label(title -> {
                    title.setText("Carrinho");
                    title.setStyle(GluonStyles.textBold(20, GluonColors.TEXT_DEFAULT));
                });
                dom.label(subtitle -> {
                    subtitle.setText("Seus produtos selecionados");
                    subtitle.setStyle(GluonStyles.text(13, GluonColors.TEXT_SECONDARY));
                });
            });
        });

        // Empty cart state
        this.emptyPane = dom.vbox(empty -> {
            empty.setAlignment(Pos.CENTER);
            empty.setSpacing(8);
            empty.setPadding(new Insets(40, 0, 40, 0));
            VBox.setVgrow(empty, Priority.ALWAYS);

            // Circular accent-light icon container (like Flutter)
            dom.stackPane(emptyIcon -> {
                emptyIcon.setStyle("-fx-background-color: " + GluonColors.ACCENT_SURFACE + "; " +
                        "-fx-background-radius: 50; -fx-min-width: 100; -fx-min-height: 100; " +
                        "-fx-max-width: 100; -fx-max-height: 100;");
                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 40, GluonColors.PRIMARY));
            });

            dom.vSpacer(20);

            dom.label(msg -> {
                msg.setText("Carrinho vazio");
                msg.setStyle(GluonStyles.textBold(16, GluonColors.TEXT_DEFAULT));
            });

            dom.label(hint -> {
                hint.setText("Adicione produtos para começar");
                hint.setStyle(GluonStyles.text(13, GluonColors.TEXT_SECONDARY));
            });

            dom.vSpacer(16);

            dom.button(shopBtn -> {
                shopBtn.setText("Ver produtos");
                shopBtn.setGraphic(GluonIcons.create(GluonIcons.STORE, 18, GluonColors.TEXT_ON_PRIMARY));
                shopBtn.setStyle(GluonStyles.BTN_PRIMARY);
                shopBtn.setOnAction(e -> safeAction("Go shopping", this.presenter::onOpenProducts));
            });
        });

        // Cart content
        this.contentPane = dom.vbox(content -> {
            content.setSpacing(10);
            content.setPadding(new Insets(0, 16, 12, 16));
            VBox.setVgrow(content, Priority.ALWAYS);

            dom.scrollVBox((sp, itemsBox) -> {
                VBox.setVgrow(sp, Priority.ALWAYS);
                sp.setFitToWidth(true);
                sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                sp.setStyle(GluonStyles.SCROLL_TRANSPARENT);

                itemsBox.setSpacing(0);
                this.itemsSlot = this.newListSlot(itemsBox, this::newItemView, this::updateItem);
            });

            // Footer: total row
            dom.hbox(footerRow -> {
                footerRow.setAlignment(Pos.CENTER_RIGHT);
                footerRow.setSpacing(4);
                footerRow.setPadding(new Insets(16, 0, 0, 0));
                footerRow.setStyle("-fx-border-color: " + GluonColors.BORDER + "; -fx-border-width: 1 0 0 0;");

                dom.label(totalLabel -> {
                    totalLabel.setText("Total: ");
                    totalLabel.setStyle(GluonStyles.text(14, GluonColors.TEXT_SECONDARY));
                });

                this.totalCostElm = dom.label(total -> {
                    total.setText(NumberFormat.getCurrencyInstance().format(computeTotalCost()));
                    total.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: " + GluonColors.PRIMARY + ";");
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

            // Action buttons row
            dom.hbox(actionRow -> {
                actionRow.setAlignment(Pos.CENTER);
                actionRow.setSpacing(8);
                actionRow.setPadding(new Insets(16, 0, 0, 0));

                dom.button(backBtn -> {
                    backBtn.setText("Continuar comprando");
                    backBtn.setGraphic(GluonIcons.create(GluonIcons.ARROW_BACK, 18, GluonColors.PRIMARY));
                    backBtn.setStyle(GluonStyles.BACK_BUTTON + " -fx-font-size: 14;");
                    backBtn.setOnAction(e -> safeAction("Back", this.presenter::onOpenProducts));
                });

                dom.button(buyBtn -> {
                    buyBtn.setText("Finalizar pedido");
                    buyBtn.setGraphic(GluonIcons.create(GluonIcons.CHECK_CIRCLE, 18, GluonColors.TEXT_ON_PRIMARY));
                    buyBtn.setStyle(GluonStyles.BTN_SUCCESS_BLOCK);
                    buyBtn.setOnAction(e -> safeAction("Buy", this.presenter::onBuy));
                });
            });
        });
    }

    private double computeTotalCost() {
        if (this.state.items == null)
            return 0;
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
        private javafx.scene.control.Button minusBtnRef;

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
            double subtotal = this.item.price * this.item.quantity;
            this.priceElm.setText(NumberFormat.getCurrencyInstance().format(subtotal));
            this.qtyElm.setText(String.valueOf(this.item.quantity));
            // Disable minus button when qty = 1 (matching Flutter's null onPressed)
            this.minusBtnRef.setDisable(this.item.quantity <= 1);
        }

        private void buildUI(GluonDom dom, HBox row) {
            row.setSpacing(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(GluonStyles.CART_ITEM_ROW);

            this.nameElm = dom.label(name -> {
                name.setStyle(GluonStyles.textBold(14, GluonColors.TEXT_DEFAULT));
                HBox.setHgrow(name, Priority.ALWAYS);
                name.setWrapText(false);
            });

            // Minus button — 28×28 icon, disabled when qty = 1 (Flutter: null onPressed)
            this.minusBtnRef = dom.button(minusBtn -> {
                minusBtn.setGraphic(GluonIcons.create(GluonIcons.MINUS, 16, GluonColors.TEXT_SECONDARY));
                minusBtn.setStyle(GluonStyles.BTN_ICON_SM);
                minusBtn.setOnAction(e -> safeAction("Qty",
                        () -> this.presenter.onModifyQuantity(this.item.id, this.item.quantity - 1)));
            });

            this.qtyElm = dom.label(qty -> {
                qty.setStyle(GluonStyles.textBold(14, GluonColors.TEXT_DEFAULT));
                qty.setMinWidth(24);
                qty.setPrefWidth(24);
                qty.setAlignment(Pos.CENTER);
            });

            // Plus button — 28×28 icon
            dom.button(plusBtn -> {
                plusBtn.setGraphic(GluonIcons.create(GluonIcons.PLUS, 16, GluonColors.TEXT_SECONDARY));
                plusBtn.setStyle(GluonStyles.BTN_ICON_SM);
                plusBtn.setOnAction(e -> safeAction("Qty",
                        () -> this.presenter.onModifyQuantity(this.item.id, this.item.quantity + 1)));
            });

            dom.hSpacer(4);

            this.priceElm = dom.label(price -> {
                price.setStyle(GluonStyles.textBold(13, GluonColors.PRIMARY));
                price.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            });

            // Delete button — 28×28 close icon, danger color
            dom.button(btn -> {
                btn.setGraphic(GluonIcons.create(GluonIcons.CLOSE, 16, GluonColors.ERROR));
                btn.setStyle(GluonStyles.BTN_ICON_SM);
                btn.setOnAction(e -> safeAction("Remove",
                        () -> this.presenter.onRemoveProduct(this.item.id)));
            });
        }
    }
}
