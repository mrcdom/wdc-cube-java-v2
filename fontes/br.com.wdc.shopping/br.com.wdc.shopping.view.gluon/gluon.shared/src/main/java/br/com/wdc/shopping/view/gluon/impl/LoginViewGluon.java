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
import javafx.scene.layout.StackPane;

public class LoginViewGluon extends AbstractViewGluon<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private TextField userNameField;
    private PasswordField passwordField;
    private Label errorElm;

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
        // ---- Left panel: gradient + brand + features ----
        dom.vbox(leftPanel -> {
            HBox.setHgrow(leftPanel, Priority.ALWAYS);
            leftPanel.setMaxWidth(Double.MAX_VALUE);
            leftPanel.setAlignment(Pos.CENTER);
            leftPanel.setStyle(GluonStyles.LOGIN_GRADIENT);

            dom.vbox(centerContent -> {
                centerContent.setAlignment(Pos.CENTER);
                centerContent.setSpacing(8);
                centerContent.setPadding(new Insets(32));

                // Glass icon box with shopping bag
                dom.stackPane(iconBox -> {
                    iconBox.setStyle(GluonStyles.LOGIN_ICON_BOX_LG);
                    dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 40, GluonColors.TEXT_ON_PRIMARY));
                });

                dom.vSpacer(16);

                dom.label(appName -> {
                    appName.setText("WDC Shopping");
                    appName.setStyle(GluonStyles.textBold(28, GluonColors.TEXT_ON_PRIMARY));
                });

                dom.label(tagline -> {
                    tagline.setText("Sua compra certa na internet.");
                    tagline.setStyle(GluonStyles.text(16, GluonColors.TEXT_ON_PRIMARY_DIM));
                });

                dom.vSpacer(32);

                // Feature rows
                buildFeatureRow(dom, "Compra segura");
                dom.vSpacer(12);
                buildFeatureRow(dom, "Entrega rápida");
                dom.vSpacer(12);
                buildFeatureRow(dom, "Troca garantida");
            });
        });

        // ---- Right panel: centered form card ----
        dom.stackPane(rightPanel -> {
            HBox.setHgrow(rightPanel, Priority.ALWAYS);
            rightPanel.setMaxWidth(Double.MAX_VALUE);
            rightPanel.setStyle("-fx-background-color: white;");

            dom.vbox(scrollWrapper -> {
                scrollWrapper.setAlignment(Pos.CENTER);
                scrollWrapper.setMaxWidth(460);
                StackPane.setAlignment(scrollWrapper, Pos.CENTER);

                // Card
                dom.vbox(card -> {
                    card.setMaxWidth(460);
                    card.setStyle(GluonStyles.CARD);

                    // Card gradient header
                    dom.vbox(cardHeader -> {
                        cardHeader.setAlignment(Pos.CENTER);
                        cardHeader.setSpacing(8);
                        cardHeader.setPadding(new Insets(28, 16, 28, 16));
                        cardHeader.setStyle(GluonStyles.LOGIN_GRADIENT);

                        dom.stackPane(iconBox -> {
                            iconBox.setStyle(GluonStyles.LOGIN_ICON_BOX);
                            dom.icon(GluonIcons.create(GluonIcons.SHOPPING_BAG, 24, GluonColors.TEXT_ON_PRIMARY));
                        });

                        dom.label(appName -> {
                            appName.setText("WDC Shopping");
                            appName.setStyle(GluonStyles.textBold(18, GluonColors.TEXT_ON_PRIMARY));
                        });

                        dom.label(tagline -> {
                            tagline.setText("Sua compra certa na internet.");
                            tagline.setStyle(GluonStyles.text(12, GluonColors.TEXT_ON_PRIMARY_DIM));
                        });
                    });

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

                            dom.label(l -> {
                                l.setText("Acesso demo: ");
                                l.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY));
                            });
                            dom.label(l -> {
                                l.setText("admin");
                                l.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY));
                            });
                            dom.label(l -> {
                                l.setText(" / ");
                                l.setStyle(GluonStyles.text(12, GluonColors.TEXT_SECONDARY));
                            });
                            dom.label(l -> {
                                l.setText("admin");
                                l.setStyle(GluonStyles.textBold(12, GluonColors.PRIMARY));
                            });
                        });
                    });
                });
            });
        });
    }

    private void buildFeatureRow(GluonDom dom, String text) {
        dom.hbox(row -> {
            row.setAlignment(Pos.CENTER_LEFT);
            row.setSpacing(8);
            dom.icon(GluonIcons.create(GluonIcons.CHECK_CIRCLE, 20, GluonColors.TEXT_ON_PRIMARY_BRIGHT));
            dom.label(lbl -> {
                lbl.setText(text);
                lbl.setStyle(GluonStyles.text(14, GluonColors.TEXT_ON_PRIMARY_BRIGHT));
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
