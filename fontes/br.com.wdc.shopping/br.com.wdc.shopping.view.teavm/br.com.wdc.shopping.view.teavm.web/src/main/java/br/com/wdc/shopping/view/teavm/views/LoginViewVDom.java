package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
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
        this.element.getClassList().add("d-flex", "justify-content-center", "align-items-center", "vh-100");
        this.element.setAttribute("style", "background:#f5f5f5");
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

        String disabledStyle = "border-radius:8px;border:1px solid #ccc;font-size:1rem"
                + (loading ? ";opacity:0.6;pointer-events:none" : "");

        return div("card shadow mx-auto")
                .style("max-width:400px;width:calc(100% - 32px);border-radius:16px;border:none;overflow:hidden")
                .children(
                        // Header azul com logo
                        div("")
                                .style("background:#4285f4;padding:1.5rem 2rem;display:flex;align-items:center;gap:0.75rem")
                                .children(
                                        span("bi bi-cart3")
                                                .style("color:#ff9800;font-size:1.75rem"),
                                        div("").children(
                                                span("")
                                                        .style("color:#fff;font-size:1.5rem;font-weight:700;letter-spacing:0.5px")
                                                        .text("Shopping"),
                                                div("")
                                                        .style("color:rgba(255,255,255,0.65);font-size:0.75rem")
                                                        .text("by WeDoCode"))),

                        // Body
                        div("").style("padding:2rem 2rem 1.5rem").children(
                                // Lock icon
                                div("text-center mb-3").children(
                                        span("bi bi-lock-fill")
                                                .style("display:inline-flex;align-items:center;justify-content:center;"
                                                        + "width:48px;height:48px;border-radius:50%;background:#e8eaf6;"
                                                        + "color:#1976d2;font-size:1.5rem")),

                                // Título
                                h5("text-center fw-bold mb-4")
                                        .style("color:#333;font-size:1.25rem")
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
                                                .style(disabledStyle)
                                                .ref(el -> this.userNameField = (HTMLInputElement) el)),

                                // Campo senha
                                div("mb-4").children(
                                        input("password", "form-control form-control-lg") // NOSONAR java:S2068
                                                .attr("placeholder", "Senha")
                                                .attr("autocomplete", "off")
                                                .attr("disabled", loading ? "true" : null)
                                                .style(disabledStyle)
                                                .on("keydown", (KeyboardEvent evt) -> {
                                                    if ("Enter".equals(evt.getKey())) {
                                                        emitEnter();
                                                    }
                                                })
                                                .ref(el -> this.passwordField = (HTMLInputElement) el)),

                                // Botão Entrar
                                button("btn btn-primary w-100 fw-bold")
                                        .style("border-radius:8px;padding:0.75rem;font-size:1.1rem;"
                                                + "background:#4285f4;border:none"
                                                + (loading ? ";opacity:0.7;pointer-events:none" : ""))
                                        .text(loading ? "Entrando..." : "Entrar")
                                        .attr("disabled", loading ? "true" : null)
                                        .on("click", evt -> emitEnter()),

                                // Separador
                                div("")
                                        .style("margin:1.5rem 0 1rem;border-top:1px solid #e0e0e0"),

                                // Hint demo
                                div("text-center")
                                        .style("border:1px dashed #ccc;border-radius:8px;padding:0.6rem 1rem;"
                                                + "color:#888;font-size:0.85rem")
                                        .children(
                                                textNode("Acesso demo: usuário "),
                                                span("fw-bold").style("color:#555").text("admin"),
                                                textNode(" / senha "),
                                                span("fw-bold").style("color:#555").text("admin"))));
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getValue();
            this.state.password = this.passwordField.getValue();
            this.presenter.onEnter();
        });
    }
}
