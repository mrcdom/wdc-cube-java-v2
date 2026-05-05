package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
            buildUI();
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

    private void buildUI() {
        var root = (VBox) this.element;
        root.setStyle("-fx-background-color: #f5f5f5;");

        // AppBar - elevated with shadow
        var appBar = new HBox(10);
        appBar.setAlignment(Pos.CENTER_LEFT);
        appBar.setPadding(new Insets(12, 16, 12, 16));
        appBar.setStyle("-fx-background-color: #1976D2; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 2);");

        var greeting = new Label("Olá,");
        greeting.setStyle("-fx-text-fill: rgba(255,255,255,0.8); -fx-font-size: 12;");

        this.nickNameElm = new Label(this.state.nickName);
        this.nickNameElm.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        this.nickNameOldValue = this.state.nickName;

        var userBox = new VBox(-2, greeting, this.nickNameElm);

        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Cart button with badge
        this.cartCountElm = new Label(String.valueOf(this.state.cartItemCount));
        this.cartCountElm.setStyle("-fx-background-color: #FF5252; -fx-text-fill: white; " +
                "-fx-font-size: 10; -fx-font-weight: bold; -fx-padding: 2 6; " +
                "-fx-background-radius: 10; -fx-min-width: 18; -fx-alignment: center;");
        this.cartCountOldValue = this.state.cartItemCount;

        var cartIcon = new Label("\uD83D\uDED2");
        cartIcon.setStyle("-fx-font-size: 18;");

        var cartBtnBox = new HBox(4, cartIcon, this.cartCountElm);
        cartBtnBox.setAlignment(Pos.CENTER);
        cartBtnBox.setPadding(new Insets(6, 12, 6, 12));
        cartBtnBox.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 20; -fx-cursor: hand;");
        cartBtnBox.setOnMouseClicked(e -> safeAction("Open cart", this.presenter::onOpenCart));

        var exitBtn = new Button("Sair");
        exitBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.9); " +
                "-fx-font-size: 12; -fx-padding: 6 10; -fx-cursor: hand;");
        exitBtn.setOnAction(e -> safeAction("Exit", this.presenter::onExit));

        appBar.getChildren().addAll(userBox, spacer, cartBtnBox, exitBtn);

        // Error label
        this.errorElm = new Label();
        this.errorElm.setStyle("-fx-text-fill: white; -fx-font-size: 12; -fx-padding: 8 16; " +
                "-fx-background-color: #d32f2f;");
        this.errorElm.setVisible(false);
        this.errorElm.setManaged(false);
        this.errorElm.setWrapText(true);
        this.errorElm.setMaxWidth(Double.MAX_VALUE);

        // Panels (stacked - mobile shows one at a time)
        this.productsPanelSlot = new StackPane();
        this.purchasesPanelSlot = new StackPane();
        this.purchasesPanelSlot.setVisible(false);
        this.purchasesPanelSlot.setManaged(false);

        this.defaultContentPane = new VBox();
        this.defaultContentPane.getChildren().addAll(this.productsPanelSlot, this.purchasesPanelSlot);
        VBox.setVgrow(this.defaultContentPane, Priority.ALWAYS);

        // Content pane (for product detail, cart, receipt)
        this.contentPane = new StackPane();
        this.contentPane.getChildren().add(this.defaultContentPane);
        VBox.setVgrow(this.contentPane, Priority.ALWAYS);

        // Bottom navigation - Material Design style
        this.tabProductsBtn = new Button("🏪  Produtos");
        this.tabProductsBtn.setMaxWidth(Double.MAX_VALUE);
        this.tabProductsBtn.setMinHeight(52);
        HBox.setHgrow(this.tabProductsBtn, Priority.ALWAYS);
        this.tabProductsBtn.setOnAction(e -> switchTab(true));

        this.tabPurchasesBtn = new Button("📋  Histórico");
        this.tabPurchasesBtn.setMaxWidth(Double.MAX_VALUE);
        this.tabPurchasesBtn.setMinHeight(52);
        HBox.setHgrow(this.tabPurchasesBtn, Priority.ALWAYS);
        this.tabPurchasesBtn.setOnAction(e -> switchTab(false));

        updateTabStyles();

        var bottomNav = new HBox(this.tabProductsBtn, this.tabPurchasesBtn);
        bottomNav.setStyle("-fx-background-color: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, -2);");

        root.getChildren().addAll(appBar, this.errorElm, this.contentPane, bottomNav);
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
        var activeStyle = "-fx-background-color: white; -fx-text-fill: #1976D2; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-background-radius: 0; " +
                "-fx-border-color: transparent transparent #1976D2 transparent; -fx-border-width: 0 0 3 0;";
        var inactiveStyle = "-fx-background-color: white; -fx-text-fill: #999; " +
                "-fx-font-size: 13; -fx-background-radius: 0; " +
                "-fx-border-color: transparent; -fx-border-width: 0 0 3 0;";
        this.tabProductsBtn.setStyle(this.showingProducts ? activeStyle : inactiveStyle);
        this.tabPurchasesBtn.setStyle(!this.showingProducts ? activeStyle : inactiveStyle);
    }
}
