package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UIImage;
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
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class LoginViewRoboVM extends AbstractViewRoboVM<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private UITextField userNameField;
    private UITextField passwordField;
    private UILabel errorLabel;

    public LoginViewRoboVM(ShoppingRoboVMApplication app, LoginPresenter presenter) {
        super("login", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.userNameField = null;
        this.passwordField = null;
        this.errorLabel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 375, 812));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        // Sync error state
        if (state.errorCode != 0) {
            errorLabel.setText(state.errorMessage != null ? state.errorMessage : "Erro desconhecido");
            errorLabel.setHidden(false);
            state.errorCode = 0;
            state.errorMessage = null;
        }
    }

    @SuppressWarnings("unused")
	private void initialRender(UIKitDom dom, UIView root) {
        // Doodle background
        dom.absolute(375, 812, doodleBg -> {
            doodleBg.setBackgroundColor(UIColor.fromRGBA(0.78, 0.80, 0.85, 1.0));
            var doodleImage = UIImage.getImage("doodle-pattern");
            if (doodleImage != null) {
                dom.imageView(375, 812, iv -> {
                    iv.setImage(doodleImage);
                    iv.setContentMode(UIViewContentMode.ScaleAspectFill);
                    iv.setAlpha(0.08);
                });
            }
        });

        // Navy header area
        dom.absolute(375, 180, headerBg -> {
            headerBg.setBackgroundColor(UIColor.fromRGBA(0.11, 0.22, 0.45, 1.0));
        });

        // Logo image
        var logoImage = UIImage.getImage("logo");
        if (logoImage != null) {
            dom.imageView(200, 22, logo -> {
                logo.setFrame(new CGRect(87, 70, 200, 22));
                logo.setImage(logoImage);
            });
        }

        // Subtitle
        dom.label(335, 22, subtitle -> {
            subtitle.setFrame(new CGRect(20, 98, 335, 22));
            subtitle.setText("Seu marketplace favorito");
            subtitle.setFont(UIFont.getSystemFont(15));
            subtitle.setTextColor(UIColor.fromRGBA(1.0, 1.0, 1.0, 0.7));
            subtitle.setTextAlignment(NSTextAlignment.Center);
        });

        // Welcome text
        dom.label(343, 30, welcome -> {
            welcome.setFrame(new CGRect(16, 200, 343, 30));
            welcome.setText("Bem-vindo!");
            welcome.setFont(UIFont.getBoldSystemFont(26));
            welcome.setTextColor(UIColor.white());
            welcome.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
            welcome.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
            welcome.setTextAlignment(NSTextAlignment.Center);
        });

        // Grouped form card
        dom.absolute(343, 97, formCard -> {
            formCard.setFrame(new CGRect(16, 248, 343, 97));
            formCard.setBackgroundColor(UIColor.white());
            formCard.getLayer().setCornerRadius(10);

            dom.textField(311, 48, field -> {
                this.userNameField = field;
                field.setFrame(new CGRect(16, 0, 311, 48));
                field.setBorderStyle(UITextBorderStyle.None);
                field.setPlaceholder("Usuário");
                field.setAutocapitalizationType(UITextAutocapitalizationType.None);
                field.setFont(UIFont.getSystemFont(17));
            });

            dom.separator(327, 0.5).setFrame(new CGRect(16, 48, 327, 0.5));

            dom.textField(311, 48, field -> {
                this.passwordField = field;
                field.setFrame(new CGRect(16, 49, 311, 48));
                field.setBorderStyle(UITextBorderStyle.None);
                field.setPlaceholder("Senha");
                field.setSecureTextEntry(true);
                field.setFont(UIFont.getSystemFont(17));
            });
        });

        // Error label
        dom.label(343, 28, error -> {
            this.errorLabel = error;
            error.setFrame(new CGRect(16, 358, 343, 28));
            error.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
            error.setFont(UIFont.getBoldSystemFont(14));
            error.setTextAlignment(NSTextAlignment.Center);
            error.setBackgroundColor(UIColor.white());
            error.getLayer().setCornerRadius(8);
            error.setClipsToBounds(true);
            error.setHidden(true);
        });

        // Login button
        dom.button(343, 50, loginBtn -> {
            loginBtn.setFrame(new CGRect(16, 398, 343, 50));
            loginBtn.setTitle("Entrar", UIControlState.Normal);
            loginBtn.setTitleColor(UIColor.white(), UIControlState.Normal);
            loginBtn.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            loginBtn.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
            loginBtn.getLayer().setCornerRadius(10);
            loginBtn.addOnTouchUpInsideListener((control, event) -> {
                safeAction("login", () -> {
                    state.userName = userNameField.getText();
                    state.password = passwordField.getText();
                    presenter.onEnter();
                });
            });
        });
    }
}
