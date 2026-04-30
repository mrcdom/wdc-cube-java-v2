package br.com.wdc.shopping.view.jfx.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import br.com.wdc.shopping.view.jfx.util.ResourceCatalog;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HomeViewJfx extends AbstractViewJfx<HomePresenter> {

    private final HomeViewState state;

    private boolean notRendered = true;
    private Label nickNameElm;
    private String nickNameOldValue;
    private Label cartCountElm;
    private int cartCountOldValue;
    private StackPane contentPane;
    private HBox defaultContentPane;
    private StackPane productsPanelSlot;
    private StackPane purchasesPanelSlot;
    private AbstractViewJfx<?> currentContentView;
    private Label errorElm;

    public HomeViewJfx(ShoppingJfxApplication app, HomePresenter presenter) {
        super("home", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nickNameOldValue, this.state.nickName)) {
            this.nickNameElm.setText("Bem-vindo, " + this.state.nickName + "!");
            this.nickNameOldValue = this.state.nickName;
        }

        if (this.cartCountOldValue != this.state.cartItemCount) {
            this.cartCountElm.setText("[" + this.state.cartItemCount + "]");
            this.cartCountOldValue = this.state.cartItemCount;
        }

        // Update products panel
        if (this.state.productsPanelView instanceof AbstractViewJfx<?> ppv) {
            if (ppv.getElement().getParent() != this.productsPanelSlot) {
                this.productsPanelSlot.getChildren().setAll(ppv.getElement());
            }
        }

        // Update purchases panel
        if (this.state.purchasesPanelView instanceof AbstractViewJfx<?> ppv) {
            if (ppv.getElement().getParent() != this.purchasesPanelSlot) {
                this.purchasesPanelSlot.getChildren().setAll(ppv.getElement());
            }
        }

        // Update content slot
        var newContentView = this.state.contentView instanceof AbstractViewJfx<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.getChildren().clear();
            if (newContentView != null) {
                this.contentPane.getChildren().add(newContentView.getElement());
            } else {
                this.contentPane.getChildren().add(this.defaultContentPane);
            }
            this.currentContentView = newContentView;
        }

        // Error
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

    @SuppressWarnings("unused")
    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("home-view");

        // Header
        dom.hbox(header -> {
            header.getStyleClass().add("header");
            header.setAlignment(Pos.CENTER_LEFT);

            dom.img(img -> {
                img.setImage(ResourceCatalog.getImage("images/logo.png"));
                img.setPreserveRatio(true);
                img.setFitHeight(30);
            });

            dom.hSpacer();

            dom.label(label -> {
                this.nickNameElm = label;
                this.nickNameElm.getStyleClass().add("welcome-label");
                this.nickNameElm.setText("Bem-vindo, " + this.state.nickName + "!");
                this.nickNameOldValue = this.state.nickName;
            });

            dom.hSpacer(10);

            dom.hbox(cartBtn -> {
                cartBtn.getStyleClass().add("cart-button");
                cartBtn.setAlignment(Pos.CENTER);
                cartBtn.setOnMouseClicked(_ -> this.presenter.onOpenCart());

                dom.img(img -> {
                    img.setImage(ResourceCatalog.getImage("images/carrinho.png"));
                    img.setFitWidth(24);
                    img.setFitHeight(24);
                });

                dom.label(label -> {
                    label.setText("Carrinho");
                    label.getStyleClass().add("cart-label");
                });

                dom.label(label -> {
                    this.cartCountElm = label;
                    this.cartCountElm.getStyleClass().add("cart-count");
                    this.cartCountElm.setText("[" + this.state.cartItemCount + "]");
                    this.cartCountOldValue = this.state.cartItemCount;
                });
            });

            dom.hSpacer(10);

            dom.button(button -> {
                button.getStyleClass().add("exit-button");
                button.setText("SAIR");
                button.setOnAction(this::emitExit);
            });
        });

        // Error
        dom.label(label -> {
            this.errorElm = label;
            this.errorElm.getStyleClass().add("error");
            this.errorElm.setVisible(false);
            this.errorElm.setManaged(false);
        });

        // Default content (products + purchases side by side)
        this.defaultContentPane = new HBox(16);
        this.defaultContentPane.getStyleClass().add("home-default-content");

        this.purchasesPanelSlot = new StackPane();
        this.productsPanelSlot = new StackPane();
        HBox.setHgrow(this.productsPanelSlot, Priority.ALWAYS);

        this.defaultContentPane.getChildren().addAll(this.productsPanelSlot, this.purchasesPanelSlot);

        // Content pane wrapped in ScrollPane
        this.contentPane = new StackPane();
        this.contentPane.setPadding(new javafx.geometry.Insets(16));
        this.contentPane.getChildren().add(this.defaultContentPane);

        var scrollPane = new ScrollPane(this.contentPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        pane0.getChildren().add(scrollPane);
    }

    private void emitExit(ActionEvent evt) {
        this.presenter.onExit();
    }
}
