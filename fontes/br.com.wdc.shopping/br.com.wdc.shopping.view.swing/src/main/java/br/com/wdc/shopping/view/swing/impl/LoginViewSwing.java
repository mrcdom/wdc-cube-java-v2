package br.com.wdc.shopping.view.swing.impl;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.ResourceCatalog;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class LoginViewSwing extends AbstractViewSwing<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JLabel errorElm;

    public LoginViewSwing(LoginPresenter presenter) {
        super("login", (ShoppingSwingApplication) presenter.app, presenter, new JPanel(new java.awt.GridBagLayout()));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.userNameField = null;
        this.passwordField = null;
        this.errorElm = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
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
        }
    }

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_LOGIN_GRADIENT_TOP);
        pane0.setBorder(new EmptyBorder(40, 40, 40, 40));

        dom.vbox(card -> {
            card.setBackground(Styles.BG_WHITE);
            card.setOpaque(true);
            card.setBorder(BorderFactory.createCompoundBorder(
                    Styles.createCardBorder(),
                    new EmptyBorder(36, 48, 32, 48)));
            card.setMaximumSize(new Dimension(440, Integer.MAX_VALUE));
            card.setPreferredSize(new Dimension(440, 520));

            // Logo
            dom.img(logo -> {
                logo.setIcon(ResourceCatalog.getScaledImage("images/big_logo.png", 160, -1));
                logo.setAlignmentX(Component.LEFT_ALIGNMENT);
                logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
            });

            dom.vSpacer(12);

            dom.label(title -> {
                title.setText("Bem-vindo!");
                title.setFont(Styles.FONT_LOGIN_TITLE);
                title.setForeground(Styles.FG_TEXT_DARK);
                title.setAlignmentX(Component.LEFT_ALIGNMENT);
                title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            });

            dom.vSpacer(4);

            dom.label(subtitle -> {
                subtitle.setText("Informe suas credenciais para acessar o sistema");
                subtitle.setFont(Styles.FONT_SUBTITLE);
                subtitle.setForeground(Styles.FG_TEXT_SUBTLE);
                subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
                subtitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            });

            dom.vSpacer(24);

            // Thin separator
            var separator = new JPanel();
            separator.setBackground(Styles.BORDER_LIGHT);
            separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            separator.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(separator);

            dom.vSpacer(20);

            // Fields
            dom.label(userLabel -> {
                userLabel.setText("Usuário");
                userLabel.setFont(Styles.FONT_FIELD_LABEL);
                userLabel.setForeground(new java.awt.Color(0x555555));
                userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                userLabel.setBorder(new EmptyBorder(0, 2, 4, 2));
            });

            dom.textField(field -> {
                this.userNameField = field;
                Styles.styleField(field);
                field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                field.setAlignmentX(Component.LEFT_ALIGNMENT);
            });

            dom.vSpacer(12);

            dom.label(passLabel -> {
                passLabel.setText("Senha");
                passLabel.setFont(Styles.FONT_FIELD_LABEL);
                passLabel.setForeground(new java.awt.Color(0x555555));
                passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                passLabel.setBorder(new EmptyBorder(0, 2, 4, 2));
            });

            dom.passwordField(field -> {
                this.passwordField = field;
                Styles.styleField(field);
                field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                field.setAlignmentX(Component.LEFT_ALIGNMENT);
                field.addActionListener(_ignored -> emitEnter());
            });

            dom.vSpacer(6);

            // Error
            dom.label(errorLabel -> {
                this.errorElm = errorLabel;
                Styles.styleErrorLabel(errorLabel);
                errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                errorLabel.setVisible(false);
                errorLabel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Styles.BORDER_ERROR, 1),
                        new EmptyBorder(8, 8, 8, 8)));
            });

            dom.vSpacer(20);

            // Button (centered)
            dom.hbox(btnRow -> {
                btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

                dom.hSpacer();

                dom.button(loginBtn -> {
                    loginBtn.setText("ENTRAR");
                    Styles.stylePrimaryButton(loginBtn);
                    loginBtn.setPreferredSize(new Dimension(200, 44));
                    loginBtn.setMaximumSize(new Dimension(200, 44));
                    loginBtn.addActionListener(_ignored -> emitEnter());
                });

                dom.hSpacer();
            });
        });
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getText();
            this.state.password = new String(this.passwordField.getPassword());
            this.presenter.onEnter();
        });
    }
}
