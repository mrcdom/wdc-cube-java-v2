package br.com.wdc.shopping.view.vaadin.impl;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class LoginViewVaadin extends AbstractViewVaadin<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private TextField userField;
    private TextField passwordField;
    private Div errorDiv;

    public LoginViewVaadin(LoginPresenter presenter) {
        super("login", (ShoppingVaadinApplication) presenter.app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.userField = null;
        this.passwordField = null;
        this.errorDiv = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.state.errorCode != 0) {
            this.errorDiv.setText("Usuário ou senha inválidos. Tente novamente.");
            this.errorDiv.setVisible(true);
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
    }

    private void initialRender(VaadinDom dom, VerticalLayout pane0) {
        pane0.addClassName("login-form");
        pane0.setSizeFull();
        pane0.setAlignItems(FlexComponent.Alignment.CENTER);
        pane0.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        dom.verticalLayout(card -> {
            card.addClassName("login-card");
            card.setMaxWidth("400px");
            card.setPadding(false);
            card.setSpacing(false);
            card.getElement().setAttribute("data-form-type", "other");

            // Blue header with SVG logo
            dom.div(header -> {
                header.addClassName("login-header");

                var logo = new Image("images/shopping-logo.svg", "WDC Shopping");
                logo.setHeight("48px");
                header.add(logo);
            });

            // Body
            dom.div(body -> {
                body.addClassName("login-body");
                // Tell Chrome this is NOT a login/signup form
                body.getElement().setAttribute("data-form-type", "other");

                // Lock icon circle
                var iconCircle = new Div();
                iconCircle.addClassName("login-icon-circle");
                var lockIcon = VaadinIcon.LOCK.create();
                lockIcon.setSize("22px");
                lockIcon.setColor("white");
                iconCircle.add(lockIcon);
                body.add(iconCircle);

                // Title
                var title = new Div();
                title.addClassName("login-title");
                title.setText("Acesso ao sistema");
                body.add(title);

                // User field
                this.userField = new TextField("Usuário");
                this.userField.setWidthFull();
                this.userField.setAutocomplete(com.vaadin.flow.component.textfield.Autocomplete.OFF);
                this.userField.getElement().executeJs("this.inputElement.setAttribute('autocomplete', 'one-time-code')");
                body.add(this.userField);

                // Password field — uses TextField with CSS masking to prevent
                // Chrome from detecting it as a password field and triggering
                // the password manager (which causes page reloads via Vaadin Push)
                this.passwordField = new TextField("Senha");
                this.passwordField.setWidthFull();
                this.passwordField.setAutocomplete(com.vaadin.flow.component.textfield.Autocomplete.OFF);
                this.passwordField.getElement().executeJs(
                        "this.inputElement.setAttribute('autocomplete', 'one-time-code');"
                        + "this.inputElement.style.webkitTextSecurity = 'disc';"
                        + "this.inputElement.style.textSecurity = 'disc';");
                this.passwordField.addKeyPressListener(Key.ENTER, e -> doLogin());
                body.add(this.passwordField);

                // Error message
                this.errorDiv = new Div();
                this.errorDiv.addClassName("error");
                this.errorDiv.setVisible(false);
                this.errorDiv.getStyle().set("margin-top", "8px");
                body.add(this.errorDiv);

                // Login button
                var loginButton = new Button("Entrar", e -> doLogin());
                loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                loginButton.setWidthFull();
                loginButton.getStyle()
                        .set("margin-top", "16px")
                        .set("border-radius", "8px")
                        .set("font-weight", "bold")
                        .set("font-size", "1rem");
                body.add(loginButton);

                // Demo hint
                var demoHint = new Div();
                demoHint.addClassName("login-demo-hint");
                demoHint.getElement().setProperty("innerHTML",
                        "Acesso demo: usuário <strong>admin</strong> / senha <strong>admin</strong>");
                body.add(demoHint);
            });
        });
    }

    private void doLogin() {
        safeAction("Login", () -> {
            this.state.userName = this.userField.getValue();
            this.state.password = this.passwordField.getValue();
            this.presenter.onEnter();
        });
    }
}
