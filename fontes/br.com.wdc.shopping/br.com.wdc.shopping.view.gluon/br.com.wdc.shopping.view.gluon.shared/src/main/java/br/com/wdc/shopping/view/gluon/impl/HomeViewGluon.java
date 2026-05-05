package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomeViewGluon extends AbstractViewGluon<HomePresenter> {

    private final HomeViewState state;

    private boolean notRendered = true;
    private Label nickNameElm;
    private String nickNameOldValue;
    private Label cartCountElm;
    private int cartCountOldValue;
    private StackPane contentPane;
    private VBox defaultContentPane;
    private StackPane productsPanelSlot;
    private StackPane purchasesPanelSlot;
    private AbstractViewGluon<?> currentContentView;
    private Label errorElm;
    private Button tabProductsBtn;
    private Button tabPurchasesBtn;
    private boolean showingProducts = true;

    public HomeViewGluon(ShoppingGluonApplication app, HomePresenter presenter) {
        super("home", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((VBox) this.element, this::buildUI);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nickNameOldValue, this.state.nickName)) {
            this.nickNameElm.setText(this.state.nickName);
            this.nickNameOldValue = this.state.nickName;
        }

        if (this.cartCountOldValue != this.state.cartItemCount) {
            this.cartCountElm.setText(String.valueOf(this.state.cartItemCount));
            this.cartCountOldValue = this.state.cartItemCount;
        }

        if (this.state.productsPanelView instanceof AbstractViewGluon<?> ppv
                && ppv.getElement().getParent() != this.productsPanelSlot) {
            this.productsPanelSlot.getChildren().setAll(ppv.getElement());
        }

        if (this.state.purchasesPanelView instanceof AbstractViewGluon<?> ppv
                && ppv.getElement().getParent() != this.purchasesPanelSlot) {
            this.purchasesPanelSlot.getChildren().setAll(ppv.getElement());
        }

        var newContentView = this.state.contentView instanceof AbstractViewGluon<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.getChildren().clear();
            if (newContentView != null) {
                this.contentPane.getChildren().add(newContentView.getElement());
            } else {
                this.contentPane.getChildren().add(this.defaultContentPane);
            }
            this.currentContentView = newContentView;
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
        root.setStyle(GluonStyles.PAGE_BG);

        // AppBar
        dom.hbox(appBar -> {
            appBar.setAlignment(Pos.CENTER_LEFT);
            appBar.setSpacing(10);
            appBar.setPadding(new Insets(12, 16, 12, 16));
            appBar.setStyle(GluonStyles.APP_BAR_PRIMARY);

            dom.vbox(userBox -> {
                userBox.setSpacing(-2);

                dom.label(greeting -> {
                    greeting.setText("Olá,");
                    greeting.setStyle(GluonStyles.TEXT_SMALL_WHITE);
                });

                this.nickNameElm = dom.label(nick -> {
                    nick.setText(this.state.nickName);
                    nick.setStyle(GluonStyles.TEXT_WHITE_BOLD);
                });
                this.nickNameOldValue = this.state.nickName;
            });

            dom.hSpacer();

            // Cart button with badge
            dom.hbox(cartBtnBox -> {
                cartBtnBox.setAlignment(Pos.CENTER);
                cartBtnBox.setSpacing(4);
                cartBtnBox.setPadding(new Insets(6, 12, 6, 12));
                cartBtnBox.setStyle(GluonStyles.CART_BTN_BOX);
                cartBtnBox.setOnMouseClicked(e -> safeAction("Open cart", this.presenter::onOpenCart));

                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_CART, 18, GluonColors.TEXT_ON_PRIMARY));

                this.cartCountElm = dom.label(badge -> {
                    badge.setText(String.valueOf(this.state.cartItemCount));
                    badge.setStyle(GluonStyles.BADGE_CART);
                });
                this.cartCountOldValue = this.state.cartItemCount;
            });

            dom.button(exitBtn -> {
                exitBtn.setText("Sair");
                exitBtn.setStyle(GluonStyles.BTN_GHOST_WHITE);
                exitBtn.setOnAction(e -> safeAction("Exit", this.presenter::onExit));
            });
        });

        // Error label
        this.errorElm = dom.label(err -> {
            err.setStyle(GluonStyles.ERROR_BAR);
            err.setVisible(false);
            err.setManaged(false);
            err.setWrapText(true);
            err.setMaxWidth(Double.MAX_VALUE);
        });

        // Content pane
        this.contentPane = dom.stackPane(cp -> {
            VBox.setVgrow(cp, Priority.ALWAYS);
            this.defaultContentPane = dom.vbox(dp -> {
                VBox.setVgrow(dp, Priority.ALWAYS);
                this.productsPanelSlot = dom.stackPane(slot -> {});
                this.purchasesPanelSlot = dom.stackPane(slot -> {
                    slot.setVisible(false);
                    slot.setManaged(false);
                });
            });
        });

        // Bottom navigation
        dom.hbox(bottomNav -> {
            bottomNav.setStyle(GluonStyles.BOTTOM_NAV);

            this.tabProductsBtn = dom.button(btn -> {
                btn.setText("Produtos");
                btn.setGraphic(GluonIcons.create(GluonIcons.STORE, 16, GluonColors.PRIMARY));
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setMinHeight(52);
                HBox.setHgrow(btn, Priority.ALWAYS);
                btn.setOnAction(e -> switchTab(true));
            });

            this.tabPurchasesBtn = dom.button(btn -> {
                btn.setText("Histórico");
                btn.setGraphic(GluonIcons.create(GluonIcons.HISTORY, 16, GluonColors.TEXT_MUTED));
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setMinHeight(52);
                HBox.setHgrow(btn, Priority.ALWAYS);
                btn.setOnAction(e -> switchTab(false));
            });
        });

        updateTabStyles();
    }

    private void switchTab(boolean showProducts) {
        if (this.currentContentView != null) {
            safeAction("Back to home", () -> Routes.home(this.app));
        }
        this.showingProducts = showProducts;
        this.productsPanelSlot.setVisible(showProducts);
        this.productsPanelSlot.setManaged(showProducts);
        this.purchasesPanelSlot.setVisible(!showProducts);
        this.purchasesPanelSlot.setManaged(!showProducts);
        updateTabStyles();
    }

    private void updateTabStyles() {
        this.tabProductsBtn.setStyle(this.showingProducts ? GluonStyles.TAB_ACTIVE : GluonStyles.TAB_INACTIVE);
        this.tabPurchasesBtn.setStyle(!this.showingProducts ? GluonStyles.TAB_ACTIVE : GluonStyles.TAB_INACTIVE);
    }
}
