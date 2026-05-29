package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.Swc.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

/**
 * Login view implementada com Spectrum Web Components + Virtual DOM.
 */
public class LoginViewVDom extends AbstractVDomView<LoginPresenter> {

    private final LoginViewState state;
    private HTMLInputElement userNameField;
    @SuppressWarnings("java:S2068")
    private HTMLInputElement passwordField;

    public LoginViewVDom(LoginPresenter presenter) {
        super("login", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
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

        // @formatter:off
        return div("login-root")
                .style("display:flex;flex:1;min-height:0;overflow:hidden")
                .children(
                    // Left decorative panel (hidden on mobile)
                    div("md-show")
                        .style("flex:1;background:linear-gradient(160deg, #0d66d0 0%, #1a8cff 40%, #4da6ff 100%);display:flex;flex-direction:column;justify-content:center;align-items:center;padding:48px;position:relative;overflow:hidden")
                        .children(
                            // Decorative circles
                            div("").style("position:absolute;top:-60px;right:-60px;width:200px;height:200px;background:rgba(255,255,255,0.06);border-radius:50%"),
                            div("").style("position:absolute;bottom:-40px;left:-40px;width:160px;height:160px;background:rgba(255,255,255,0.04);border-radius:50%"),
                            div("").style("position:absolute;top:40%;left:20%;width:80px;height:80px;background:rgba(255,255,255,0.05);border-radius:50%"),
                            // Content
                            div("")
                                .style("text-align:center;position:relative;z-index:1")
                                .children(
                                    div("")
                                        .style("width:80px;height:80px;background:rgba(255,255,255,0.12);border-radius:20px;display:inline-flex;align-items:center;justify-content:center;margin-bottom:24px;backdrop-filter:blur(8px);border:1px solid rgba(255,255,255,0.2)")
                                        .children(span("bi bi-bag-check").style("font-size:2.2rem;color:#fff")),
                                    div("")
                                        .style("font-size:2rem;font-weight:800;color:#fff;letter-spacing:-0.5px;margin-bottom:8px")
                                        .children(textNode("WDC Shopping")),
                                    div("")
                                        .style("font-size:0.9rem;color:rgba(255,255,255,0.8);line-height:1.6;max-width:280px")
                                        .children(textNode("Sua compra certa na internet.")),
                                    // Feature list
                                    div("")
                                        .style("margin-top:32px;text-align:left;display:inline-flex;flex-direction:column;gap:12px")
                                        .children(
                                            renderFeature("bi bi-shield-check", "Compra segura"),
                                            renderFeature("bi bi-truck", "Entrega rápida"),
                                            renderFeature("bi bi-arrow-repeat", "Troca garantida")))),

                    // Right: form panel
                    div("login-card")
                        .style("width:100%;max-width:460px;display:flex;flex-direction:column;justify-content:center;align-items:center;padding:32px;background:var(--app-surface)")
                        .children(
                            div("")
                                .style("width:100%;max-width:320px")
                                .children(
                                    // Mobile-only logo (blue gradient banner like desktop panel)
                                    div("md-hide")
                                        .style("text-align:center;padding:24px 16px;margin:-32px -32px 24px -32px;background:linear-gradient(160deg, #0d66d0 0%, #1a8cff 40%, #4da6ff 100%);position:relative;overflow:hidden")
                                        .children(
                                            div("").style("position:absolute;top:-30px;right:-30px;width:100px;height:100px;background:rgba(255,255,255,0.06);border-radius:50%"),
                                            div("").style("position:absolute;bottom:-20px;left:-20px;width:80px;height:80px;background:rgba(255,255,255,0.04);border-radius:50%"),
                                            div("")
                                                .style("position:relative;z-index:1")
                                                .children(
                                                    div("")
                                                        .style("width:48px;height:48px;background:rgba(255,255,255,0.12);border-radius:12px;display:inline-flex;align-items:center;justify-content:center;margin-bottom:8px;backdrop-filter:blur(8px);border:1px solid rgba(255,255,255,0.2)")
                                                        .children(span("bi bi-bag-check").style("font-size:1.4rem;color:#fff")),
                                                    div("")
                                                        .style("font-size:1.2rem;font-weight:700;color:#fff")
                                                        .children(textNode("WDC Shopping")),
                                                    div("")
                                                        .style("font-size:0.75rem;color:rgba(255,255,255,0.7);margin-top:4px")
                                                        .children(textNode("Sua compra certa na internet.")))),

                                    // Welcome text
                                    div("")
                                        .style("margin-bottom:28px")
                                        .children(
                                            div("")
                                                .style("font-size:1.5rem;font-weight:700;color:var(--app-text);margin-bottom:4px")
                                                .children(textNode("Bem-vindo")),
                                            div("")
                                                .style("font-size:0.85rem;color:var(--app-text-secondary)")
                                                .children(textNode("Entre com suas credenciais para continuar"))),

                                    // Alerta de erro
                                    div("")
                                        .style(showError ? "display:flex;align-items:center;gap:10px;padding:12px 16px;background:#fef2f2;border:1px solid #fecaca;border-radius:var(--app-radius-sm);margin-bottom:1rem" : "display:none")
                                        .children(
                                            span("bi bi-exclamation-circle").style("color:#dc2626;font-size:1rem;flex-shrink:0"),
                                            span("").style("font-size:0.85rem;color:#991b1b;font-weight:500").text(errorMessage)),

                                    // Campo usuário
                                    spFieldLabel("login-user", "Usuário")
                                        .style("font-weight:500"),
                                    spTextField("Digite seu usuário")
                                        .attr("id", "login-user")
                                        .attr("autocomplete", "off")
                                        .boolAttr("disabled", loading)
                                        .style("width:100%;margin-bottom:16px")
                                        .ref(el -> this.userNameField = (HTMLInputElement) el),

                                    // Campo senha
                                    spFieldLabel("login-pass", "Senha")
                                        .style("font-weight:500"),
                                    spTextField("Digite sua senha", "password")
                                        .attr("id", "login-pass")
                                        .attr("autocomplete", "off")
                                        .boolAttr("disabled", loading)
                                        .style("width:100%;margin-bottom:24px")
                                        .on("keydown", (KeyboardEvent evt) -> {
                                            if ("Enter".equals(evt.getKey())) {
                                                emitEnter();
                                            }
                                        })
                                        .ref(el -> this.passwordField = (HTMLInputElement) el),

                                    // Botão Entrar
                                    spButton("accent", "l")
                                        .boolAttr("disabled", loading)
                                        .boolAttr("pending", loading)
                                        .style("width:100%;margin-bottom:20px")
                                        .on("click", evt -> emitEnter())
                                        .children(textNode(loading ? "Entrando..." : "Entrar")),

                                    // Hint demo
                                    div("")
                                        .style("text-align:center;padding:12px;background:var(--app-bg);border-radius:var(--app-radius-sm);border:1px solid var(--app-border)")
                                        .children(
                                            span("").style("font-size:0.8rem;color:var(--app-text-secondary)").text("Acesso demo: "),
                                            span("").style("font-size:0.8rem;font-weight:700;color:var(--app-accent)").text("admin"),
                                            span("").style("font-size:0.8rem;color:var(--app-text-secondary)").text(" / "),
                                            span("").style("font-size:0.8rem;font-weight:700;color:var(--app-accent)").text("admin")))));
        // @formatter:on
    }

    private VNode renderFeature(String icon, String text) {
        // @formatter:off
        return div("")
                .style("display:flex;align-items:center;gap:10px")
                .children(
                        span(icon).style("font-size:1rem;color:rgba(255,255,255,0.9)"),
                        span("").style("font-size:0.85rem;color:rgba(255,255,255,0.85);font-weight:500").text(text));
        // @formatter:on
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getValue();
            this.state.password = this.passwordField.getValue();
            this.presenter.onEnter();
        });
    }
}
