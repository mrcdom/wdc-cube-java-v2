package br.com.wdc.shopping.view.jfx.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import br.com.wdc.shopping.view.jfx.util.ResourceCatalog;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginViewJfx extends AbstractViewJfx<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private TextField userNameField;
    private PasswordField passwordField;
    private Label errorElm;

    public LoginViewJfx(ShoppingJfxApplication app, LoginPresenter presenter) {
        super("login", app, presenter, new VBox());
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
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

    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("login-form");
        pane0.setAlignment(Pos.CENTER);

        dom.vbox(pane1 -> {
            pane1.getStyleClass().add("login-card");
            pane1.setAlignment(Pos.CENTER);
            pane1.setMaxWidth(500);

            dom.img(img -> {
                img.setImage(ResourceCatalog.getImage("images/big_logo.png"));
                img.setPreserveRatio(true);
                img.setFitWidth(312);
            });

            dom.label(label -> {
                label.setText("Usuário:");
                label.getStyleClass().add("field-label");
            });

            dom.textField(field -> {
                this.userNameField = field;
                field.setPromptText("Usuário");
                field.getStyleClass().add("login-field");
            });

            dom.label(label -> {
                label.setText("Senha:");
                label.getStyleClass().add("field-label");
            });

            dom.passwordField(field -> {
                this.passwordField = field;
                field.setPromptText("Senha");
                field.getStyleClass().add("login-field");
                field.setOnAction(this::emitEnter);
            });

            dom.label(label -> {
                this.errorElm = label;
                this.errorElm.getStyleClass().add("error");
                this.errorElm.setVisible(false);
                this.errorElm.setManaged(false);
            });

            dom.button(button -> {
                button.getStyleClass().add("login-button");
                button.setText("ENTRAR");
                button.setOnAction(this::emitEnter);
            });
        });
    }

    private void emitEnter(ActionEvent evt) {
        this.state.userName = this.userNameField.getText();
        this.state.password = this.passwordField.getText();
        this.presenter.onEnter();
    }
}
