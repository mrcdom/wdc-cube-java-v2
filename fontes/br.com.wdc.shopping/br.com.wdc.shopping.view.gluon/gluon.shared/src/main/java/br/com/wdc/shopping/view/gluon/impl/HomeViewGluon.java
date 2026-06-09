package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
    private HBox defaultContentPane;
    private StackPane productsPanelSlot;
    private StackPane purchasesPanelSlot;
    private AbstractViewGluon<?> currentContentView;
    private Label errorElm;

    public HomeViewGluon(HomePresenter presenter) {
        super("home", (ShoppingGluonApplication) presenter.app, presenter, new VBox());
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

        // AppBar — gradient with exit icon | greeting | [spacer] | logo | [spacer] | cart
        dom.hbox(appBar -> {
            appBar.setAlignment(Pos.CENTER_LEFT);
            appBar.setSpacing(8);
            appBar.setPadding(new Insets(8, 12, 8, 12));
            appBar.setStyle(GluonStyles.APP_BAR_PRIMARY);

            // Exit icon button
            dom.button(exitBtn -> {
                exitBtn.setGraphic(GluonIcons.create(GluonIcons.LOGOUT, 20, GluonColors.TEXT_ON_PRIMARY));
                exitBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 4;");
                exitBtn.setOnAction(e -> safeAction("Exit", this.presenter::onExit));
            });

            dom.hSpacer(4);

            // Greeting column
            dom.vbox(greetingBox -> {
                greetingBox.setSpacing(-2);

                dom.label(greeting -> {
                    greeting.setText("Bem-vindo(a),");
                    greeting.setStyle(GluonStyles.text(11, GluonColors.TEXT_ON_PRIMARY_DIM));
                });

                this.nickNameElm = dom.label(nick -> {
                    nick.setText(this.state.nickName);
                    nick.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_ON_PRIMARY));
                });
                this.nickNameOldValue = this.state.nickName;
            });

            dom.hSpacer();

            // Center logo
            dom.hbox(logoBox -> {
                logoBox.setAlignment(Pos.CENTER);
                logoBox.setSpacing(6);

                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 24, GluonColors.TEXT_ON_PRIMARY));

                dom.vbox(logoText -> {
                    logoText.setSpacing(-2);
                    dom.label(l -> {
                        l.setText("Shopping");
                        l.setStyle(GluonStyles.textBold(15, GluonColors.TEXT_ON_PRIMARY));
                    });
                    dom.label(l -> {
                        l.setText("By WeDoCode");
                        l.setStyle(GluonStyles.text(10, "rgba(255,255,255,0.6)"));
                    });
                });
            });

            dom.hSpacer();

            // Cart button with white badge
            dom.hbox(cartArea -> {
                cartArea.setAlignment(Pos.CENTER);
                cartArea.setSpacing(6);
                cartArea.setPadding(new Insets(4, 8, 4, 8));
                cartArea.setStyle(GluonStyles.CART_BTN_BOX);
                cartArea.setOnMouseClicked(e -> safeAction("Open cart", this.presenter::onOpenCart));

                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 24, GluonColors.TEXT_ON_PRIMARY));

                dom.label(l -> {
                    l.setText("Carrinho");
                    l.setStyle(GluonStyles.textBold(14, GluonColors.TEXT_ON_PRIMARY));
                });

                dom.hSpacer(8);

                this.cartCountElm = dom.label(badge -> {
                    badge.setText(String.valueOf(this.state.cartItemCount));
                    badge.setStyle(GluonStyles.BADGE_CART);
                });
                this.cartCountOldValue = this.state.cartItemCount;
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
            this.defaultContentPane = dom.hbox(dp -> {
                VBox.setVgrow(dp, Priority.ALWAYS);
                this.productsPanelSlot = dom.stackPane(slot -> {
                    HBox.setHgrow(slot, Priority.ALWAYS);
                });
                // Purchases panel: fixed 340 px wide, white surface with left border
                this.purchasesPanelSlot = dom.stackPane(slot -> {
                    slot.setPrefWidth(340);
                    slot.setMinWidth(300);
                    slot.setMaxWidth(400);
                    slot.setStyle(GluonStyles.PURCHASES_PANEL);
                });
            });
        });
    }
}
