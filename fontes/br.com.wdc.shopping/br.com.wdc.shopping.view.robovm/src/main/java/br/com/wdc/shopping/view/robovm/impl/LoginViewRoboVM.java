package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIImage;
import org.robovm.apple.uikit.UIImageView;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UITextField;
import org.robovm.apple.uikit.UITextAutocapitalizationType;
import org.robovm.apple.uikit.UITextBorderStyle;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewContentMode;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class LoginViewRoboVM extends AbstractViewRoboVM<LoginPresenter> {

    private final LoginViewState state;
    private boolean built;

    private UITextField userNameField;
    private UITextField passwordField;
    private UILabel errorLabel;

    public LoginViewRoboVM(ShoppingRoboVMApplication app, LoginPresenter presenter) {
        super("login", app, presenter);
        this.state = presenter.state;
    }

    private void buildUI() {
        var container = new UIView(new CGRect(0, 0, 375, 812));

        // Doodle background - same as Home
        var doodleBg = new UIView(new CGRect(0, 0, 375, 812));
        doodleBg.setBackgroundColor(UIColor.fromRGBA(0.78, 0.80, 0.85, 1.0));
        var doodleImage = UIImage.getImage("doodle-pattern");
        if (doodleImage != null) {
            var doodleImageView = new UIImageView(new CGRect(0, 0, 375, 812));
            doodleImageView.setImage(doodleImage);
            doodleImageView.setContentMode(UIViewContentMode.ScaleAspectFill);
            doodleImageView.setAlpha(0.08);
            doodleBg.addSubview(doodleImageView);
        }
        container.addSubview(doodleBg);

        // Navy header area
        var headerBg = new UIView(new CGRect(0, 0, 375, 180));
        headerBg.setBackgroundColor(UIColor.fromRGBA(0.11, 0.22, 0.45, 1.0));
        container.addSubview(headerBg);

        // Logo image - centered in header
        var logoImage = UIImage.getImage("logo");
        if (logoImage != null) {
            var logoView = new UIImageView(new CGRect(87, 70, 200, 22));
            logoView.setImage(logoImage);
            logoView.setContentMode(UIViewContentMode.ScaleAspectFit);
            container.addSubview(logoView);
        }

        // Subtitle below logo - white on navy
        var subtitleLabel = new UILabel(new CGRect(20, 98, 335, 22));
        subtitleLabel.setText("Seu marketplace favorito");
        subtitleLabel.setFont(UIFont.getSystemFont(15));
        subtitleLabel.setTextColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.7));
        subtitleLabel.setTextAlignment(NSTextAlignment.Center);
        container.addSubview(subtitleLabel);

        // Welcome text on doodle area
        var welcomeLabel = new UILabel(new CGRect(16, 200, 343, 30));
        welcomeLabel.setText("Bem-vindo!");
        welcomeLabel.setFont(UIFont.getBoldSystemFont(26));
        welcomeLabel.setTextColor(UIColor.white());
        welcomeLabel.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        welcomeLabel.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        welcomeLabel.setTextAlignment(NSTextAlignment.Center);
        container.addSubview(welcomeLabel);

        // Grouped form card (white, consistent with other screens)
        var formCard = new UIView(new CGRect(16, 248, 343, 97));
        formCard.setBackgroundColor(UIColor.white());
        formCard.getLayer().setCornerRadius(10);
        container.addSubview(formCard);

        // Username field - no border, inside card
        userNameField = new UITextField(new CGRect(16, 0, 311, 48));
        userNameField.setBorderStyle(UITextBorderStyle.None);
        userNameField.setPlaceholder("Usuário");
        userNameField.setAutocapitalizationType(UITextAutocapitalizationType.None);
        userNameField.setFont(UIFont.getSystemFont(17));
        formCard.addSubview(userNameField);

        // Separator between fields
        var fieldSep = new UIView(new CGRect(16, 48, 327, 0.5));
        fieldSep.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
        formCard.addSubview(fieldSep);

        // Password field
        passwordField = new UITextField(new CGRect(16, 49, 311, 48));
        passwordField.setBorderStyle(UITextBorderStyle.None);
        passwordField.setPlaceholder("Senha");
        passwordField.setSecureTextEntry(true);
        passwordField.setFont(UIFont.getSystemFont(17));
        formCard.addSubview(passwordField);

        // Error label - white card for readability on doodle
        errorLabel = new UILabel(new CGRect(16, 358, 343, 28));
        errorLabel.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
        errorLabel.setFont(UIFont.getBoldSystemFont(14));
        errorLabel.setTextAlignment(NSTextAlignment.Center);
        errorLabel.setBackgroundColor(UIColor.white());
        errorLabel.getLayer().setCornerRadius(8);
        errorLabel.setClipsToBounds(true);
        errorLabel.setHidden(true);
        container.addSubview(errorLabel);

        // Login button - green, consistent with other action buttons
        var loginButton = new UIButton(UIButtonType.System);
        loginButton.setFrame(new CGRect(16, 398, 343, 50));
        loginButton.setTitle("Entrar", UIControlState.Normal);
        loginButton.setTitleColor(UIColor.white(), UIControlState.Normal);
        loginButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
        loginButton.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
        loginButton.getLayer().setCornerRadius(10);
        loginButton.addOnTouchUpInsideListener((control, event) -> {
            safeAction("login", () -> {
                state.userName = userNameField.getText();
                state.password = passwordField.getText();
                presenter.onEnter();
            });
        });
        container.addSubview(loginButton);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Sync error state
        if (state.errorCode != 0) {
            errorLabel.setText(state.errorMessage != null ? state.errorMessage : "Erro desconhecido");
            errorLabel.setHidden(false);
            state.errorCode = 0;
            state.errorMessage = null;
        }
    }
}
