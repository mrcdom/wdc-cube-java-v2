package br.com.wdc.shopping.view.gluon.impl;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.gluon.AbstractViewGluon;
import br.com.wdc.shopping.view.gluon.ShoppingGluonApplication;
import br.com.wdc.shopping.view.gluon.util.ResourceCatalog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
            buildUI();
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

    private void buildUI() {
        var root = (VBox) this.element;
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(0));
        root.setSpacing(0);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1976D2, #1565C0);");

        // Top spacer
        var topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // Logo
        var logoView = new ImageView(ResourceCatalog.getImage("images/big_logo.png"));
        logoView.setFitWidth(160);
        logoView.setPreserveRatio(true);

        // App name
        var appName = new Label("WDC Shopping");
        appName.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: white;");

        var tagline = new Label("Sua loja favorita na palma da mão");
        tagline.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.8);");

        // Header section
        var headerBox = new VBox(8, logoView, appName, tagline);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 24, 24, 24));

        // Card form
        var card = new VBox(14);
        card.setMaxWidth(320);
        card.setPadding(new Insets(28, 24, 28, 24));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16 16 0 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, -2);");

        var formTitle = new Label("Entrar");
        formTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #333;");

        var formSubtitle = new Label("Informe suas credenciais para continuar");
        formSubtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #888;");

        this.userNameField = new TextField();
        this.userNameField.setPromptText("Usuário");
        this.userNameField.setMaxWidth(Double.MAX_VALUE);
        this.userNameField.setStyle("-fx-padding: 10; -fx-background-radius: 6; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-font-size: 13;");

        this.passwordField = new PasswordField();
        this.passwordField.setPromptText("Senha");
        this.passwordField.setMaxWidth(Double.MAX_VALUE);
        this.passwordField.setStyle("-fx-padding: 10; -fx-background-radius: 6; " +
                "-fx-border-color: #e0e0e0; -fx-border-radius: 6; -fx-font-size: 13;");
        this.passwordField.setOnAction(e -> emitEnter());

        this.errorElm = new Label();
        this.errorElm.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11; -fx-padding: 4 0 0 0;");
        this.errorElm.setVisible(false);
        this.errorElm.setManaged(false);
        this.errorElm.setWrapText(true);

        var loginBtn = new Button("ENTRAR");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 14; -fx-padding: 12 20; -fx-background-radius: 8; " +
                "-fx-cursor: hand;");
        loginBtn.setOnAction(e -> emitEnter());

        var footerNote = new Label("demo: admin / admin");
        footerNote.setStyle("-fx-font-size: 10; -fx-text-fill: #aaa;");

        card.getChildren().addAll(formTitle, formSubtitle,
                this.userNameField, this.passwordField,
                this.errorElm, loginBtn, footerNote);

        // Bottom spacer to push card down
        var bottomSpacer = new Region();
        bottomSpacer.setMinHeight(0);

        root.getChildren().addAll(topSpacer, headerBox, card);
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getText();
            this.state.password = this.passwordField.getText();
            this.presenter.onEnter();
        });
    }
}
