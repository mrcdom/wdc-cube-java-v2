package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.theme.GluonColors;
import br.com.wdc.shopping.view.gluon.util.GluonDom;
import br.com.wdc.shopping.view.gluon.theme.GluonStyles;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginViewGluon extends AbstractViewGluon<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private TextField userNameField;
    private PasswordField passwordField;
    private Label errorElm;

    public LoginViewGluon(ShoppingGluonApplication app, LoginPresenter presenter) {
        super("login", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            GluonDom.render((VBox) this.element, this::buildUI);
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

    private void buildUI(GluonDom dom, VBox root) {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle(GluonStyles.LOGIN_GRADIENT);

        dom.vSpacer();

        // Header section
        dom.vbox(headerBox -> {
            headerBox.setAlignment(Pos.CENTER);
            headerBox.setSpacing(8);
            headerBox.setPadding(new Insets(0, 24, 24, 24));

            dom.imageView(logo -> {
                logo.setImage(ResourceCatalog.getImage("images/big_logo.png"));
                logo.setFitWidth(160);
                logo.setPreserveRatio(true);
            });

            dom.label(appName -> {
                appName.setText("WDC Shopping");
                appName.setStyle(GluonStyles.textBold(24, GluonColors.TEXT_ON_PRIMARY));
            });

            dom.label(tagline -> {
                tagline.setText("Sua loja favorita na palma da mão");
                tagline.setStyle(GluonStyles.TEXT_SMALL_WHITE);
            });
        });

        // Card form
        dom.vbox(card -> {
            card.setMaxWidth(320);
            card.setSpacing(14);
            card.setPadding(new Insets(28, 24, 28, 24));
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle(GluonStyles.CARD_TOP_ROUND);

            dom.label(formTitle -> {
                formTitle.setText("Entrar");
                formTitle.setStyle(GluonStyles.textBold(18, GluonColors.TEXT_DEFAULT));
            });

            dom.label(formSubtitle -> {
                formSubtitle.setText("Informe suas credenciais para continuar");
                formSubtitle.setStyle(GluonStyles.TEXT_HINT_STYLE);
            });

            this.userNameField = dom.textField(field -> {
                field.setPromptText("Usuário");
                field.setMaxWidth(Double.MAX_VALUE);
                field.setStyle(GluonStyles.INPUT_FIELD);
            });

            this.passwordField = dom.passwordField(field -> {
                field.setPromptText("Senha");
                field.setMaxWidth(Double.MAX_VALUE);
                field.setStyle(GluonStyles.INPUT_FIELD);
                field.setOnAction(e -> emitEnter());
            });

            this.errorElm = dom.label(err -> {
                err.setStyle(GluonStyles.ERROR_SMALL);
                err.setVisible(false);
                err.setManaged(false);
                err.setWrapText(true);
            });

            dom.button(loginBtn -> {
                loginBtn.setText("ENTRAR");
                loginBtn.setMaxWidth(Double.MAX_VALUE);
                loginBtn.setStyle(GluonStyles.BTN_PRIMARY);
                loginBtn.setOnAction(e -> emitEnter());
            });

            dom.label(footerNote -> {
                footerNote.setText("demo: admin / admin");
                footerNote.setStyle(GluonStyles.text(10, GluonColors.TEXT_DISABLED));
            });
        });
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getText();
            this.state.password = this.passwordField.getText();
            this.presenter.onEnter();
        });
    }
}
