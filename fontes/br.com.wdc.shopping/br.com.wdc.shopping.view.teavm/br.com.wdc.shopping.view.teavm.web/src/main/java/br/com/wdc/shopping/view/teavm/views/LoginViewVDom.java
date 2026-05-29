package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

/**
 * Login view implementada com Virtual DOM.
 * <p>
 * O render() descreve a UI inteira; o VDom aplica apenas as diferenças.
 */
public class LoginViewVDom extends AbstractVDomView<LoginPresenter> {

    private final LoginViewState state;
    private HTMLInputElement userNameField;
    @SuppressWarnings("java:S2068") // Not a hardcoded password — HTML input field reference
    private HTMLInputElement passwordField;

    public LoginViewVDom(LoginPresenter presenter) {
        super("login", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "justify-content-center", "align-items-center");
        this.element.setAttribute("style", "flex:1;min-height:0;background:" + SURFACE_SECONDARY);
    }

    @Override
    protected VNode render() {
        final boolean loading = this.state.loading;

        // Consumir erro do state (one-shot)
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode != 0) {
            showError = true;
            errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            showError = false;
            errorMessage = "";
        }

        String inputStyle = loading ? INPUT_DISABLED : INPUT;

        return div("card shadow mx-auto")
                .style(LOGIN_CARD)
                .children(
                        // Header azul com logo
                        div("")
                                .style(APP_HEADER)
                                .children(
                                        span(BsIcons.CART)
                                                .style(APP_LOGO_ICON),
                                        div("").children(
                                                span("")
                                                        .style(APP_LOGO_TEXT)
                                                        .text("Shopping"),
                                                div("")
                                                        .style(APP_LOGO_SUBTITLE)
                                                        .text("by WeDoCode"))),

                        // Body
                        div("").style("padding:2rem 2rem 1.5rem").children(
                                // Lock icon
                                div("text-center mb-3").children(
                                        span("bi bi-lock-fill")
                                                .style(LOCK_ICON)),

                                // Título
                                h5("text-center fw-bold mb-4")
                                        .style("color:" + TEXT_DARK + ";font-size:1.25rem")
                                        .text("Acesso ao sistema"),

                                // Alerta de erro (condicional)
                                div("alert alert-danger")
                                        .style(showError ? "" : "display:none")
                                        .text(errorMessage)
                                        .when(true),

                                // Campo usuário
                                div("mb-3").children(
                                        input("text", "form-control form-control-lg")
                                                .attr("placeholder", "Usuário")
                                                .attr("autocomplete", "off")
                                                .attr("autocapitalize", "none")
                                                .attr("disabled", loading ? "true" : null)
                                                .style(inputStyle)
                                                .ref(el -> this.userNameField = (HTMLInputElement) el)),

                                // Campo senha
                                div("mb-4").children(
                                        input("password", "form-control form-control-lg") // NOSONAR java:S2068
                                                .attr("placeholder", "Senha")
                                                .attr("autocomplete", "off")
                                                .attr("disabled", loading ? "true" : null)
                                                .style(inputStyle)
                                                .on("keydown", (KeyboardEvent evt) -> {
                                                    if ("Enter".equals(evt.getKey())) {
                                                        emitEnter();
                                                    }
                                                })
                                                .ref(el -> this.passwordField = (HTMLInputElement) el)),

                                // Botão Entrar
                                button("btn btn-primary w-100 fw-bold")
                                        .style(loading ? BTN_PRIMARY_LG_DISABLED : BTN_PRIMARY_LG)
                                        .text(loading ? "Entrando..." : "Entrar")
                                        .attr("disabled", loading ? "true" : null)
                                        .on("click", evt -> emitEnter()),

                                // Separador
                                div("")
                                        .style("margin:1.5rem 0 1rem;" + SEPARATOR),

                                // Hint demo
                                div("text-center")
                                        .style(HINT_BOX)
                                        .children(
                                                textNode("Acesso demo: usuário "),
                                                span("fw-bold").style("color:" + TEXT_DARK).text("admin"),
                                                textNode(" / senha "),
                                                span("fw-bold").style("color:" + TEXT_DARK).text("admin"))));
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getValue();
            this.state.password = this.passwordField.getValue();
            this.presenter.onEnter();
        });
    }
}
