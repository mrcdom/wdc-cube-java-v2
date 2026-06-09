package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.theme.GluonIcons;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class LoginViewGluon extends AbstractViewGluon<LoginPresenter> {

    /** Matches Flutter breakpointMd — below this, collapse to single card */
    private static final double BREAKPOINT_MD = 768.0;

    private final LoginViewState state;

    private boolean notRendered = true;
    private TextField userNameField;
    private PasswordField passwordField;
    private Label errorElm;

    /** Left gradient panel — hidden when width < BREAKPOINT_MD */
    private StackPane leftPanelElm;
    /** Right panel container — background changes between wide and narrow */
    private StackPane rightPanelElm;

    public LoginViewGluon(LoginPresenter presenter) {
        super("login", (ShoppingGluonApplication) presenter.app, presenter, new HBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((HBox) this.element, this::buildUI);
            this.notRendered = false;
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

    private void buildUI(GluonDom dom, HBox root) {
        // ---- Left panel: gradient + decorative circles + brand + features ----
        // Uses StackPane so decorative circles can be layered behind content
        this.leftPanelElm = dom.stackPane(leftPanel -> {
            HBox.setHgrow(leftPanel, Priority.ALWAYS);
            leftPanel.setMaxWidth(Double.MAX_VALUE);
            leftPanel.setStyle(GluonStyles.LOGIN_GRADIENT);

            // Decorative circles — partial transparent white overlays, same as Flutter
            var c1 = makeDecorativeCircle(200, 0.06);
            StackPane.setAlignment(c1, Pos.TOP_RIGHT);
            StackPane.setMargin(c1, new Insets(-60, -60, 0, 0));
            leftPanel.getChildren().add(c1);

            var c2 = makeDecorativeCircle(160, 0.04);
            StackPane.setAlignment(c2, Pos.BOTTOM_LEFT);
            StackPane.setMargin(c2, new Insets(0, 0, -40, -40));
            leftPanel.getChildren().add(c2);

            var c3 = makeDecorativeCircle(80, 0.05);
            StackPane.setAlignment(c3, Pos.TOP_LEFT);
            StackPane.setMargin(c3, new Insets(140, 0, 0, 60));
            leftPanel.getChildren().add(c3);

            // Content — last child = on top of circles
            dom.vbox(content -> {
                content.setAlignment(Pos.CENTER);
                content.setSpacing(8);
                content.setPadding(new Insets(32));

                // Icon box (glass effect, 80×80, radius 20)
                dom.stackPane(iconBox -> {
                    iconBox.setMinSize(80, 80);
                    iconBox.setPrefSize(80, 80);
                    iconBox.setMaxSize(80, 80);
                    iconBox.setStyle(GluonStyles.LOGIN_ICON_BOX_LG);
                    dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 40, GluonColors.TEXT_ON_PRIMARY));
                });

                dom.vSpacer(16);

                dom.label(appName -> {
                    appName.setText("WDC Shopping");
                    appName.setStyle(GluonStyles.textBold(28, GluonColors.TEXT_ON_PRIMARY));
                });

                dom.vSpacer(8);

                dom.label(tagline -> {
                    tagline.setText("Sua compra certa na internet.");
                    tagline.setStyle(GluonStyles.text(16, "rgba(255,255,255,0.8)"));
                });

                dom.vSpacer(32);

                // Feature rows — each with its own icon (matching Flutter)
                buildFeatureRow(dom, GluonIcons.SHIELD, "Compra segura");
                dom.vSpacer(12);
                buildFeatureRow(dom, GluonIcons.LOCAL_SHIPPING, "Entrega rápida");
                dom.vSpacer(12);
                buildFeatureRow(dom, GluonIcons.AUTORENEW, "Troca garantida");
            });
        });

        // ---- Right panel: white background (or appBg in compact mode) ----
        this.rightPanelElm = dom.stackPane(rightPanel -> {
            HBox.setHgrow(rightPanel, Priority.ALWAYS);
            rightPanel.setMaxWidth(Double.MAX_VALUE);
            rightPanel.setStyle("-fx-background-color: white;");

            // Scrollable wrapper — centers card vertically and horizontally
            dom.vbox(scrollWrapper -> {
                scrollWrapper.setAlignment(Pos.CENTER);
                scrollWrapper.setPadding(new Insets(24));
                scrollWrapper.setMaxWidth(460);
                StackPane.setAlignment(scrollWrapper, Pos.CENTER);

                // Card — large shadow, no border (Flutter _buildForm container)
                dom.vbox(card -> {
                    card.setMaxWidth(460);
                    card.setStyle(GluonStyles.CARD_LOGIN);

                    // Card gradient header — inset 24px horizontally (matches Flutter Padding(fromLTRB(24,0,24,0)))
                    // Uses StackPane for decorative circles behind content
                    var headerPane = new StackPane();
                    headerPane.setStyle(GluonStyles.LOGIN_GRADIENT);

                    var hc1 = makeDecorativeCircle(120, 0.08);
                    StackPane.setAlignment(hc1, Pos.TOP_RIGHT);
                    StackPane.setMargin(hc1, new Insets(-50, -40, 0, 0));
                    headerPane.getChildren().add(hc1);

                    var hc2 = makeDecorativeCircle(100, 0.06);
                    StackPane.setAlignment(hc2, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(hc2, new Insets(0, 0, -40, -30));
                    headerPane.getChildren().add(hc2);

                    // Header content column
                    var headerContent = new VBox();
                    headerContent.setAlignment(Pos.CENTER);
                    headerContent.setSpacing(8);
                    headerContent.setPadding(new Insets(28, 16, 28, 16));

                    var iconBox = new StackPane();
                    iconBox.setMinSize(48, 48);
                    iconBox.setPrefSize(48, 48);
                    iconBox.setMaxSize(48, 48);
                    iconBox.setStyle(GluonStyles.LOGIN_ICON_BOX);
                    iconBox.getChildren().add(GluonIcons.create(GluonIcons.SHOPPING_BAG, 24, GluonColors.TEXT_ON_PRIMARY));

                    var titleLbl = new Label("WDC Shopping");
                    titleLbl.setStyle(GluonStyles.textBold(18, GluonColors.TEXT_ON_PRIMARY));
                    var tagLbl = new Label("Sua compra certa na internet.");
                    tagLbl.setStyle(GluonStyles.text(12, "rgba(255,255,255,0.7)"));

                    headerContent.getChildren().addAll(iconBox, titleLbl, tagLbl);
                    headerPane.getChildren().add(headerContent);

                    VBox.setMargin(headerPane, new Insets(0, 24, 0, 24));
                    card.getChildren().add(headerPane);

                    // Form body
                    dom.vbox(formBody -> {
                        formBody.setSpacing(0);
                        formBody.setPadding(new Insets(28, 48, 32, 48));
                        formBody.setAlignment(Pos.TOP_LEFT);

                        dom.label(formTitle -> {
                            formTitle.setText("Bem-vindo");
                            formTitle.setStyle(GluonStyles.textBold(24, GluonColors.TEXT_DEFAULT));
                        });

                        dom.vSpacer(4);

                        dom.label(formSubtitle -> {
                            formSubtitle.setText("Entre com suas credenciais para continuar");
                            formSubtitle.setStyle(GluonStyles.text(14, GluonColors.TEXT_SECONDARY));
                            formSubtitle.setWrapText(true);
                            formSubtitle.setMaxWidth(Double.MAX_VALUE);
                        });

                        dom.vSpacer(28);

                        this.errorElm = dom.label(err -> {
                            err.setStyle(GluonStyles.ERROR_INLINE);
                            err.setVisible(false);
                            err.setManaged(false);
                            err.setWrapText(true);
                            err.setMaxWidth(Double.MAX_VALUE);
                        });

                        dom.label(userLabel -> {
                            userLabel.setText("Usuário");
                            userLabel.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_DEFAULT));
                        });

                        dom.vSpacer(4);

                        this.userNameField = dom.textField(field -> {
                            field.setPromptText("Digite seu usuário");
                            field.setMaxWidth(Double.MAX_VALUE);
                            field.setStyle(GluonStyles.INPUT_FIELD);
                        });

                        dom.vSpacer(16);

                        dom.label(passLabel -> {
                            passLabel.setText("Senha");
                            passLabel.setStyle(GluonStyles.textBold(13, GluonColors.TEXT_DEFAULT));
                        });

                        dom.vSpacer(4);

                        this.passwordField = dom.passwordField(field -> {
                            field.setPromptText("Digite sua senha");
                            field.setMaxWidth(Double.MAX_VALUE);
                            field.setStyle(GluonStyles.INPUT_FIELD);
                            field.setOnAction(e -> emitEnter());
                        });

                        dom.vSpacer(24);

                        dom.button(loginBtn -> {
                            loginBtn.setText("Entrar");
                            loginBtn.setMaxWidth(Double.MAX_VALUE);
                            loginBtn.setMinHeight(48);
                            loginBtn.setStyle(GluonStyles.BTN_PRIMARY);
                            loginBtn.setOnAction(e -> emitEnter());
                        });

                        dom.vSpacer(20);

                        // Demo credentials box
                        dom.hbox(demoBox -> {
                            demoBox.setAlignment(Pos.CENTER);
                            demoBox.setSpacing(4);
                            demoBox.setMaxWidth(Double.MAX_VALUE);
                            demoBox.setStyle(GluonStyles.DEMO_BOX);

                            dom.label(l -> l.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY))).setText("Acesso demo: ");
                            dom.label(l -> l.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY))).setText("admin");
                            dom.label(l -> l.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY))).setText(" / ");
                            dom.label(l -> l.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY))).setText("admin");
                        });
                    });
                });
            });
        });

        // Responsive: hide left panel when root width < BREAKPOINT_MD (matching Flutter LayoutBuilder)
        root.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW.doubleValue();
            if (w <= 0) return;
            boolean wide = w >= BREAKPOINT_MD;
            this.leftPanelElm.setVisible(wide);
            this.leftPanelElm.setManaged(wide);
            // Right panel: white on wide, appBg on narrow (matches Flutter Center+SingleChildScrollView on appBg)
            this.rightPanelElm.setStyle(wide ? "-fx-background-color: white;" : GluonStyles.PAGE_BG);
        });
    }

    /** Creates a translucent white circle Region — matches Flutter _decorativeCircle */
    private static Region makeDecorativeCircle(double size, double alpha) {
        var r = new Region();
        r.setMinSize(size, size);
        r.setPrefSize(size, size);
        r.setMaxSize(size, size);
        r.setStyle("-fx-background-radius: 50; -fx-background-color: rgba(255,255,255," + alpha + ");");
        return r;
    }

    private void buildFeatureRow(GluonDom dom, String iconPath, String text) {
        dom.hbox(row -> {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(8);
            dom.icon(GluonIcons.create(iconPath, 20, "rgba(255,255,255,0.9)"));
            dom.label(lbl -> {
                lbl.setText(text);
                lbl.setStyle(GluonStyles.text(14, "rgba(255,255,255,0.9)"));
            });
        });
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            var userName = this.userNameField.getText();
            var password = this.passwordField.getText();
            this.presenter.onEnter(userName, password);
        });
    }
}
