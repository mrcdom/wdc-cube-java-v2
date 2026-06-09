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
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class HomeViewGluon extends AbstractViewGluon<HomePresenter> {

    /** Breakpoints matching Flutter's design_tokens.dart */
    private static final double BREAKPOINT_SM = 576.0;
    private static final double BREAKPOINT_MD = 768.0;

    private final HomeViewState state;

    private boolean notRendered = true;
    private boolean showingProducts = true;
    private boolean wideMode = true;
    private Label nickNameElm;
    private String nickNameOldValue;
    private Label cartCountElm;
    private int cartCountOldValue;
    private StackPane contentPane;
    private VBox defaultContentPane;
    private AbstractViewGluon<?> currentContentView;
    private Label errorElm;

    // Header responsive elements (hidden below BREAKPOINT_SM)
    private VBox greetingBox;
    private Label cartLabel;

    // Compact tab nav (visible below BREAKPOINT_MD)
    private HBox tabNavRow;
    private SVGPath tabProductsIcon;
    private Label tabProductsLabel;
    private Region tabProductsIndicator;
    private SVGPath tabPurchasesIcon;
    private Label tabPurchasesLabel;
    private Region tabPurchasesIndicator;

    // Panel slots
    private HBox widePanelRow;
    private StackPane wideProductsSlot;
    private StackPane widePurchasesSlot;
    private StackPane narrowSlot;

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

        // Assign panel views to the correct slot based on current responsive mode
        if (this.state.productsPanelView instanceof AbstractViewGluon<?> ppv) {
            var targetSlot = this.wideMode ? this.wideProductsSlot
                    : (this.showingProducts ? this.narrowSlot : null);
            if (targetSlot != null && ppv.getElement().getParent() != targetSlot) {
                targetSlot.getChildren().setAll(ppv.getElement());
            }
        }

        if (this.state.purchasesPanelView instanceof AbstractViewGluon<?> ppv) {
            var targetSlot = this.wideMode ? this.widePurchasesSlot
                    : (!this.showingProducts ? this.narrowSlot : null);
            if (targetSlot != null && ppv.getElement().getParent() != targetSlot) {
                targetSlot.getChildren().setAll(ppv.getElement());
            }
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

        // AppBar — gradient header matching Flutter _HeaderPanel
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

            // Greeting column — hidden when width < BREAKPOINT_SM (like Flutter isExpanded)
            this.greetingBox = dom.vbox(gb -> {
                gb.setSpacing(-2);
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

            // Cart button — "Carrinho" label hidden when width < BREAKPOINT_SM
            dom.hbox(cartArea -> {
                cartArea.setAlignment(Pos.CENTER);
                cartArea.setSpacing(6);
                cartArea.setPadding(new Insets(4, 8, 4, 8));
                cartArea.setStyle(GluonStyles.CART_BTN_BOX);
                cartArea.setOnMouseClicked(e -> safeAction("Open cart", this.presenter::onOpenCart));

                dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 24, GluonColors.TEXT_ON_PRIMARY));

                this.cartLabel = dom.label(l -> {
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

        // Content pane (StackPane fills remaining height)
        this.contentPane = dom.stackPane(cp -> {
            VBox.setVgrow(cp, Priority.ALWAYS);

            this.defaultContentPane = dom.vbox(dp -> {
                // Tab nav — visible only in compact mode (width < BREAKPOINT_MD)
                // Matches Flutter _TabNav with icon + label + 2px indicator line
                this.tabNavRow = dom.hbox(tabRow -> {
                    tabRow.setStyle(GluonStyles.TAB_NAV);
                    tabRow.setVisible(false);
                    tabRow.setManaged(false);

                    // Products tab
                    dom.vbox(tabProducts -> {
                        HBox.setHgrow(tabProducts, Priority.ALWAYS);
                        tabProducts.setAlignment(Pos.CENTER);
                        tabProducts.setSpacing(2);
                        tabProducts.setPadding(new Insets(8, 0, 0, 0));
                        tabProducts.setStyle("-fx-cursor: hand;");
                        tabProducts.setOnMouseClicked(e -> switchTab(true));

                        this.tabProductsIcon = dom.icon(GluonIcons.create(GluonIcons.GRID_VIEW, 18, GluonColors.PRIMARY));

                        this.tabProductsLabel = dom.label(lbl -> {
                            lbl.setText("Produtos");
                            lbl.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY));
                        });

                        dom.vSpacer(4);

                        var ind = new Region();
                        ind.setMaxWidth(Double.MAX_VALUE);
                        ind.setMinHeight(2);
                        ind.setPrefHeight(2);
                        ind.setMaxHeight(2);
                        ind.setStyle("-fx-background-color: " + GluonColors.PRIMARY + ";");
                        this.tabProductsIndicator = ind;
                        dom.node(ind);
                    });

                    // Purchases tab
                    dom.vbox(tabPurchases -> {
                        HBox.setHgrow(tabPurchases, Priority.ALWAYS);
                        tabPurchases.setAlignment(Pos.CENTER);
                        tabPurchases.setSpacing(2);
                        tabPurchases.setPadding(new Insets(8, 0, 0, 0));
                        tabPurchases.setStyle("-fx-cursor: hand;");
                        tabPurchases.setOnMouseClicked(e -> switchTab(false));

                        this.tabPurchasesIcon = dom.icon(GluonIcons.create(GluonIcons.HISTORY, 18, GluonColors.TEXT_SECONDARY));

                        this.tabPurchasesLabel = dom.label(lbl -> {
                            lbl.setText("Histórico");
                            lbl.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY));
                        });

                        dom.vSpacer(4);

                        var ind = new Region();
                        ind.setMaxWidth(Double.MAX_VALUE);
                        ind.setMinHeight(2);
                        ind.setPrefHeight(2);
                        ind.setMaxHeight(2);
                        ind.setStyle("-fx-background-color: transparent;");
                        this.tabPurchasesIndicator = ind;
                        dom.node(ind);
                    });
                });

                // Body StackPane: wide HBox (side by side) or compact single slot
                dom.stackPane(bodyPane -> {
                    VBox.setVgrow(bodyPane, Priority.ALWAYS);

                    // Wide mode: products | purchases side by side (width >= BREAKPOINT_MD)
                    this.widePanelRow = dom.hbox(wideRow -> {
                        this.wideProductsSlot = dom.stackPane(slot -> {
                            HBox.setHgrow(slot, Priority.ALWAYS);
                        });
                        this.widePurchasesSlot = dom.stackPane(slot -> {
                            slot.setPrefWidth(340);
                            slot.setMinWidth(300);
                            slot.setMaxWidth(400);
                            slot.setStyle(GluonStyles.PURCHASES_PANEL);
                        });
                    });

                    // Compact mode: single active slot (hidden initially)
                    this.narrowSlot = dom.stackPane(slot -> {
                        slot.setVisible(false);
                        slot.setManaged(false);
                    });
                });
            });
        });

        // Width listener — responsive breakpoints matching Flutter's breakpointSm / breakpointMd
        root.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w <= 0) return;

            boolean showGreeting = w >= BREAKPOINT_SM;
            this.greetingBox.setVisible(showGreeting);
            this.greetingBox.setManaged(showGreeting);
            this.cartLabel.setVisible(showGreeting);
            this.cartLabel.setManaged(showGreeting);

            boolean newWide = w >= BREAKPOINT_MD;
            if (newWide != this.wideMode) {
                this.wideMode = newWide;
                this.tabNavRow.setVisible(!newWide);
                this.tabNavRow.setManaged(!newWide);
                this.widePanelRow.setVisible(newWide);
                this.widePanelRow.setManaged(newWide);
                this.narrowSlot.setVisible(!newWide);
                this.narrowSlot.setManaged(!newWide);
                reassignPanelViews();
            }
        });
    }

    /** Switch active tab in compact mode — matches Flutter _switchTab */
    private void switchTab(boolean showProducts) {
        if (this.showingProducts == showProducts) return;
        this.showingProducts = showProducts;
        updateTabStyles();
        if (!this.wideMode) {
            var active = showProducts ? this.state.productsPanelView : this.state.purchasesPanelView;
            if (active instanceof AbstractViewGluon<?> ppv) {
                this.narrowSlot.getChildren().setAll(ppv.getElement());
            }
        }
    }

    /** Update tab button active/inactive visual state */
    private void updateTabStyles() {
        String activeColor = GluonColors.PRIMARY;
        String inactiveColor = GluonColors.TEXT_SECONDARY;

        this.tabProductsIcon.setFill(Color.web(this.showingProducts ? activeColor : inactiveColor));
        this.tabProductsLabel.setStyle(GluonStyles.text(12, this.showingProducts ? activeColor : inactiveColor)
                + (this.showingProducts ? " -fx-font-weight: bold;" : ""));
        this.tabProductsIndicator.setStyle("-fx-background-color: "
                + (this.showingProducts ? activeColor : "transparent") + ";");

        this.tabPurchasesIcon.setFill(Color.web(!this.showingProducts ? activeColor : inactiveColor));
        this.tabPurchasesLabel.setStyle(GluonStyles.text(12, !this.showingProducts ? activeColor : inactiveColor)
                + (!this.showingProducts ? " -fx-font-weight: bold;" : ""));
        this.tabPurchasesIndicator.setStyle("-fx-background-color: "
                + (!this.showingProducts ? activeColor : "transparent") + ";");
    }

    /** Move panel view elements to the correct slots after mode change */
    private void reassignPanelViews() {
        if (this.wideMode) {
            if (this.state.productsPanelView instanceof AbstractViewGluon<?> ppv) {
                this.wideProductsSlot.getChildren().setAll(ppv.getElement());
            }
            if (this.state.purchasesPanelView instanceof AbstractViewGluon<?> ppv) {
                this.widePurchasesSlot.getChildren().setAll(ppv.getElement());
            }
        } else {
            var active = this.showingProducts ? this.state.productsPanelView : this.state.purchasesPanelView;
            if (active instanceof AbstractViewGluon<?> ppv) {
                this.narrowSlot.getChildren().setAll(ppv.getElement());
            }
        }
    }
}
