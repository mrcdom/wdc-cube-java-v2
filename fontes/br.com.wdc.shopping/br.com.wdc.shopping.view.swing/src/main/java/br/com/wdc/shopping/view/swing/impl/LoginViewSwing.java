package br.com.wdc.shopping.view.swing.impl;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
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

    public LoginViewSwing(ShoppingSwingApplication app, LoginPresenter presenter) {
        super("login", app, presenter, new JPanel(new BorderLayout()));
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
            initialRender();
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

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_LOGIN_GRADIENT_TOP);
        this.element.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Card
        var card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Styles.BG_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                Styles.createCardBorder(),
                new EmptyBorder(36, 48, 32, 48)));
        card.setMaximumSize(new Dimension(440, Integer.MAX_VALUE));
        card.setPreferredSize(new Dimension(440, 520));

        // Center the card
        var wrapper = new JPanel(new java.awt.GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);
        this.element.add(wrapper, BorderLayout.CENTER);

        // All children use LEFT_ALIGNMENT so BoxLayout stretches them to full width
        // Text centering is done via JLabel.setHorizontalAlignment(CENTER)

        // Logo
        var logo = new JLabel(ResourceCatalog.getScaledImage("images/big_logo.png", 160, -1));
        logo.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        logo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        card.add(logo);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        var title = new JLabel("Bem-vindo!");
        title.setFont(Styles.FONT_LOGIN_TITLE);
        title.setForeground(Styles.FG_TEXT_DARK);
        title.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 4)));

        var subtitle = new JLabel("Informe suas credenciais para acessar o sistema");
        subtitle.setFont(Styles.FONT_SUBTITLE);
        subtitle.setForeground(Styles.FG_TEXT_SUBTLE);
        subtitle.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        subtitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        subtitle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 24)));

        // Thin separator
        var separator = new JPanel();
        separator.setBackground(Styles.BORDER_LIGHT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        card.add(separator);
        card.add(Box.createRigidArea(new Dimension(0, 20)));

        // Fields
        var userLabel = new JLabel("Usuário");
        userLabel.setFont(Styles.FONT_FIELD_LABEL);
        userLabel.setForeground(new java.awt.Color(0x555555));
        userLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        userLabel.setBorder(new EmptyBorder(0, 2, 4, 2));
        card.add(userLabel);

        this.userNameField = new JTextField();
        Styles.styleField(this.userNameField);
        this.userNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        this.userNameField.setAlignmentX(JTextField.LEFT_ALIGNMENT);
        card.add(this.userNameField);
        card.add(Box.createRigidArea(new Dimension(0, 12)));

        var passLabel = new JLabel("Senha");
        passLabel.setFont(Styles.FONT_FIELD_LABEL);
        passLabel.setForeground(new java.awt.Color(0x555555));
        passLabel.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        passLabel.setBorder(new EmptyBorder(0, 2, 4, 2));
        card.add(passLabel);

        this.passwordField = new JPasswordField();
        Styles.styleField(this.passwordField);
        this.passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        this.passwordField.setAlignmentX(JPasswordField.LEFT_ALIGNMENT);
        this.passwordField.addActionListener(_ -> emitEnter());
        card.add(this.passwordField);

        // Error
        this.errorElm = new JLabel();
        Styles.styleErrorLabel(this.errorElm);
        this.errorElm.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        this.errorElm.setVisible(false);
        this.errorElm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Styles.BORDER_ERROR, 1),
                new EmptyBorder(8, 8, 8, 8)));
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(this.errorElm);

        card.add(Box.createRigidArea(new Dimension(0, 20)));

        var loginBtn = new JButton("ENTRAR");
        Styles.stylePrimaryButton(loginBtn);
        loginBtn.setPreferredSize(new Dimension(200, 44));

        var btnWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        btnWrapper.setOpaque(false);
        btnWrapper.setAlignmentX(JPanel.LEFT_ALIGNMENT);
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.addActionListener(_ -> emitEnter());
        btnWrapper.add(loginBtn);
        card.add(btnWrapper);
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getText();
            this.state.password = new String(this.passwordField.getPassword());
            this.presenter.onEnter();
        });
    }
}
