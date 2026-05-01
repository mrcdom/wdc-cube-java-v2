package br.com.wdc.shopping.view.vaadin.impl;

import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.vaadin.AbstractViewVaadin;
import br.com.wdc.shopping.view.vaadin.ShoppingVaadinApplication;
import br.com.wdc.shopping.view.vaadin.util.VaadinDom;

public class LoginViewVaadin extends AbstractViewVaadin<LoginPresenter> {

    private final LoginViewState state;

    private boolean notRendered = true;
    private LoginForm loginForm;

    public LoginViewVaadin(ShoppingVaadinApplication app, LoginPresenter presenter) {
        super("login", app, presenter, new VerticalLayout());
        this.state = presenter.state;
    }

    @Override
    public void recreate() {
        this.element = new VerticalLayout();
        this.notRendered = true;
        this.loginForm = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            VaadinDom.render((VerticalLayout) this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.state.errorCode != 0) {
            this.loginForm.setError(true);
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
            card.setAlignItems(FlexComponent.Alignment.CENTER);

            dom.image(img -> {
                img.setSrc("images/big_logo.png");
                img.setAlt("Logo");
                img.setWidth("200px");
            });

            dom.h2(h -> {
                h.setText("Bem-vindo!");
                h.getStyle().set("margin", "8px 0 0 0");
            });

            dom.span(label -> {
                label.addClassName("login-subtitle");
                label.setText("Informe suas credenciais para acessar o sistema");
            });

            var i18n = LoginI18n.createDefault();
            var form = i18n.getForm();
            form.setTitle("");
            form.setUsername("Usuário");
            form.setPassword("Senha");
            form.setSubmit("ENTRAR");
            form.setForgotPassword("");
            i18n.setForm(form);
            var errorMsg = i18n.getErrorMessage();
            errorMsg.setTitle("Login falhou");
            errorMsg.setMessage("Usuário ou senha inválidos. Tente novamente.");
            i18n.setErrorMessage(errorMsg);

            this.loginForm = new LoginForm(i18n);
            this.loginForm.setForgotPasswordButtonVisible(false);
            this.loginForm.addLoginListener(e -> safeAction("Login", () -> {
                this.state.userName = e.getUsername();
                this.state.password = e.getPassword();
                this.presenter.onEnter();
            }));
            card.add(this.loginForm);
        });
    }
}
