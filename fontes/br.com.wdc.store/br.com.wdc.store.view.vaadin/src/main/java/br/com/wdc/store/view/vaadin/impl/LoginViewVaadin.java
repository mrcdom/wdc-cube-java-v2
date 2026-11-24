package br.com.wdc.store.view.vaadin.impl;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.view.vaadin.AbstractViewVaadin;
import br.com.wdc.store.view.vaadin.StoreVaadinApplication;
import br.com.wdc.store.view.vaadin.util.VaadinDom;

public class LoginViewVaadin extends AbstractViewVaadin<LoginScope> {

    private boolean notRendered = true;
    private TextField userField;
    private TextField passwordField;
    private Div errorDiv;

    public LoginViewVaadin(StoreVaadinApplication app, LoginScope scope) {
        super("login", app, scope, new VerticalLayout());
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

        if (this.scope.errorCode.get() != 0) {
            this.errorDiv.setText("Invalid username or password. Please try again.");
            this.errorDiv.setVisible(true);
            // Consumed while drawing: a redraw now would take the message away
            this.scope.errorCode.setSilently(0);
            this.scope.errorMessage.setSilently(null);
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

                var logo = new Image("images/store-logo.svg", "WDC Store");
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
                title.setText("Account access");
                body.add(title);

                // User field
                this.userField = new TextField("Username");
                this.userField.setWidthFull();
                this.userField.setAutocomplete(com.vaadin.flow.component.textfield.Autocomplete.OFF);
                this.userField.getElement()
                        .executeJs("this.inputElement.setAttribute('autocomplete', 'one-time-code')");
                body.add(this.userField);

                // Password field — uses TextField with CSS masking to prevent
                // Chrome from detecting it as a password field and triggering
                // the password manager (which causes page reloads via Vaadin Push)
                this.passwordField = new TextField("Password");
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
                var loginButton = new Button("Sign in", e -> doLogin());
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
                        "Demo access: username <strong>admin</strong> / password <strong>admin</strong>");
                body.add(demoHint);
            });
        });
    }

    private void doLogin() {
        safeAction("Login", () -> {
            var userName = this.userField.getValue();
            var password = this.passwordField.getValue();
            this.scope.onEnter.run(userName, password);
        });
    }
}
