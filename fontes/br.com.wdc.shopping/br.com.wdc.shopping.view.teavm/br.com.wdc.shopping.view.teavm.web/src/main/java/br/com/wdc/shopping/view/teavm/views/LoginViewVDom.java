package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

/**
 * Login view implementada com Spectrum Web Components + Virtual DOM.
 */
public class LoginViewVDom extends AbstractVDomView<LoginPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String LOGIN_ROOT = css()
                .displayFlex()
                .flex("1")
                .minHeight("0")
                .overflowHidden()
                .build();

        String LEFT_PANEL = css()
                .flex("1")
                .background("linear-gradient(160deg, #0d66d0 0%, #1a8cff 40%, #4da6ff 100%)")
                .flexCol()
                .justifyContent("center")
                .alignItems("center")
                .padding("48px")
                .position("relative")
                .overflowHidden()
                .build();

        String DECO_CIRCLE_1 = css()
                .position("absolute")
                .top("-60px")
                .right("-60px")
                .width("200px")
                .height("200px")
                .background("rgba(255,255,255,0.06)")
                .borderRadius("50%")
                .build();

        String DECO_CIRCLE_2 = css()
                .position("absolute")
                .bottom("-40px")
                .left("-40px")
                .width("160px")
                .height("160px")
                .background("rgba(255,255,255,0.04)")
                .borderRadius("50%")
                .build();

        String DECO_CIRCLE_3 = css()
                .position("absolute")
                .top("40%")
                .left("20%")
                .width("80px")
                .height("80px")
                .background("rgba(255,255,255,0.05)")
                .borderRadius("50%")
                .build();

        String CONTENT_CENTER = css()
                .textAlign("center")
                .position("relative")
                .zIndex(1)
                .build();

        String LOGO_BOX_LG = css()
                .width("80px")
                .height("80px")
                .background("rgba(255,255,255,0.12)")
                .borderRadius("20px")
                .displayInlineFlex()
                .alignItems("center")
                .justifyContent("center")
                .marginBottom("24px")
                .prop("backdrop-filter", "blur(8px)")
                .border("1px solid rgba(255,255,255,0.2)")
                .build();

        String ICON_LG = css()
                .fontSize("2.2rem")
                .color("#fff")
                .build();

        String TITLE_LG = css()
                .fontSize("2rem")
                .fontWeight("800")
                .color("#fff")
                .prop("letter-spacing", "-0.5px")
                .marginBottom("8px")
                .build();

        String SUBTITLE_LG = css()
                .fontSize("0.9rem")
                .color("rgba(255,255,255,0.8)")
                .lineHeight("1.6")
                .maxWidth("280px")
                .build();

        String FEATURES_LIST = css()
                .marginTop("32px")
                .textAlign("left")
                .displayInlineFlex()
                .flexDirection("column")
                .gap("12px")
                .build();

        String FORM_PANEL = css()
                .width("100%")
                .maxWidth("460px")
                .flexCol()
                .justifyContent("center")
                .alignItems("center")
                .padding("32px")
                .background("var(--app-surface)")
                .build();

        String FORM_CONTENT = css()
                .width("100%")
                .maxWidth("320px")
                .build();

        String MOBILE_LOGO_BG = css()
                .textAlign("center")
                .padding("24px 16px")
                .margin("-32px -32px 24px -32px")
                .background("linear-gradient(160deg, #0d66d0 0%, #1a8cff 40%, #4da6ff 100%)")
                .position("relative")
                .overflowHidden()
                .build();

        String MOBILE_CIRCLE_1 = css()
                .position("absolute")
                .top("-30px")
                .right("-30px")
                .width("100px")
                .height("100px")
                .background("rgba(255,255,255,0.06)")
                .borderRadius("50%")
                .build();

        String MOBILE_CIRCLE_2 = css()
                .position("absolute")
                .bottom("-20px")
                .left("-20px")
                .width("80px")
                .height("80px")
                .background("rgba(255,255,255,0.04)")
                .borderRadius("50%")
                .build();

        String MOBILE_CONTENT = css()
                .position("relative")
                .zIndex(1)
                .build();

        String LOGO_BOX_SM = css()
                .width("48px")
                .height("48px")
                .background("rgba(255,255,255,0.12)")
                .borderRadius("12px")
                .displayInlineFlex()
                .alignItems("center")
                .justifyContent("center")
                .marginBottom("8px")
                .prop("backdrop-filter", "blur(8px)")
                .border("1px solid rgba(255,255,255,0.2)")
                .build();

        String ICON_SM = css()
                .fontSize("1.4rem")
                .color("#fff")
                .build();

        String MOBILE_TITLE = css()
                .fontSize("1.2rem")
                .fontWeight("700")
                .color("#fff")
                .build();

        String MOBILE_SUBTITLE = css()
                .fontSize("0.75rem")
                .color("rgba(255,255,255,0.7)")
                .marginTop("4px")
                .build();

        String WELCOME_WRAP = css()
                .marginBottom("28px")
                .build();

        String WELCOME_TITLE = css()
                .fontSize("1.5rem")
                .fontWeight("700")
                .color("var(--app-text)")
                .marginBottom("4px")
                .build();

        String WELCOME_SUBTITLE = css()
                .fontSize("0.85rem")
                .color("var(--app-text-secondary)")
                .build();

        String ERROR_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("12px 16px")
                .background("#fef2f2")
                .border("1px solid #fecaca")
                .borderRadius("var(--app-radius-sm)")
                .marginBottom("1rem")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String ERROR_ICON = css()
                .color("#dc2626")
                .fontSize("1rem")
                .flexShrink(0)
                .build();

        String ERROR_TEXT = css()
                .fontSize("0.85rem")
                .color("#991b1b")
                .fontWeight("500")
                .build();

        String FIELD_LABEL = css()
                .fontWeight("500")
                .build();

        String USER_FIELD = css()
                .width("100%")
                .marginBottom("16px")
                .build();

        String PASSWORD_FIELD = css()
                .width("100%")
                .marginBottom("24px")
                .build();

        String ENTER_BUTTON = css()
                .width("100%")
                .marginBottom("20px")
                .build();

        String DEMO_HINT = css()
                .textAlign("center")
                .padding("12px")
                .background("var(--app-bg)")
                .borderRadius("var(--app-radius-sm)")
                .border("1px solid var(--app-border)")
                .build();

        String DEMO_TEXT = css()
                .fontSize("0.8rem")
                .color("var(--app-text-secondary)")
                .build();

        String DEMO_HIGHLIGHT = css()
                .fontSize("0.8rem")
                .fontWeight("700")
                .color("var(--app-accent)")
                .build();

        String FEATURE_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .build();

        String FEATURE_ICON = css()
                .fontSize("1rem")
                .color("rgba(255,255,255,0.9)")
                .build();

        String FEATURE_TEXT = css()
                .fontSize("0.85rem")
                .color("rgba(255,255,255,0.85)")
                .fontWeight("500")
                .build();
    }

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
        return div("login-root").style(Styles.LOGIN_ROOT).children(
          // Left decorative panel (hidden on mobile)
          div("md-show").style(Styles.LEFT_PANEL).children(
            div().style(Styles.DECO_CIRCLE_1),
            div().style(Styles.DECO_CIRCLE_2),
            div().style(Styles.DECO_CIRCLE_3),
            div().style(Styles.CONTENT_CENTER).children(
              div().style(Styles.LOGO_BOX_LG)
                .children(span("bi bi-bag-check").style(Styles.ICON_LG)),
              div().style(Styles.TITLE_LG).children(textNode("WDC Shopping")),
              div().style(Styles.SUBTITLE_LG).children(textNode("Sua compra certa na internet.")),
              div().style(Styles.FEATURES_LIST).children(
                renderFeature("bi bi-shield-check", "Compra segura"),
                renderFeature("bi bi-truck", "Entrega rápida"),
                renderFeature("bi bi-arrow-repeat", "Troca garantida")))),
          // Right: form panel
          div("login-card").style(Styles.FORM_PANEL).children(
            div().style(Styles.FORM_CONTENT).children(
              // Mobile-only logo
              div("md-hide").style(Styles.MOBILE_LOGO_BG).children(
                div().style(Styles.MOBILE_CIRCLE_1),
                div().style(Styles.MOBILE_CIRCLE_2),
                div().style(Styles.MOBILE_CONTENT).children(
                  div().style(Styles.LOGO_BOX_SM)
                    .children(span("bi bi-bag-check").style(Styles.ICON_SM)),
                  div().style(Styles.MOBILE_TITLE).children(textNode("WDC Shopping")),
                  div().style(Styles.MOBILE_SUBTITLE).children(textNode("Sua compra certa na internet.")))),
              // Welcome text
              div().style(Styles.WELCOME_WRAP).children(
                div().style(Styles.WELCOME_TITLE).children(textNode("Bem-vindo")),
                div().style(Styles.WELCOME_SUBTITLE).children(textNode("Entre com suas credenciais para continuar"))),
              // Error alert
              div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
                span("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
                span().style(Styles.ERROR_TEXT).text(errorMessage)),
              // User field
              spFieldLabel("login-user", "Usuário").style(Styles.FIELD_LABEL),
              spTextField("Digite seu usuário")
                .attr("id", "login-user")
                .attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .style(Styles.USER_FIELD)
                .ref(el -> this.userNameField = (HTMLInputElement) el),
              // Password field
              spFieldLabel("login-pass", "Senha").style(Styles.FIELD_LABEL),
              spTextField("Digite sua senha", "password")
                .attr("id", "login-pass")
                .attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .style(Styles.PASSWORD_FIELD)
                .on("keydown", (KeyboardEvent evt) -> {
                    if ("Enter".equals(evt.getKey())) { emitEnter(); }
                })
                .ref(el -> this.passwordField = (HTMLInputElement) el),
              // Enter button
              spButton("accent", "l")
                .boolAttr("disabled", loading)
                .boolAttr("pending", loading)
                .style(Styles.ENTER_BUTTON)
                .on("click", evt -> emitEnter())
                .children(textNode(loading ? "Entrando..." : "Entrar")),
              // Demo hint
              div().style(Styles.DEMO_HINT).children(
                span().style(Styles.DEMO_TEXT).text("Acesso demo: "),
                span().style(Styles.DEMO_HIGHLIGHT).text("admin"),
                span().style(Styles.DEMO_TEXT).text(" / "),
                span().style(Styles.DEMO_HIGHLIGHT).text("admin")))));
        // @formatter:on
    }

    private VNode renderFeature(String icon, String text) {
        // @formatter:off
        return div().style(Styles.FEATURE_ROW).children(
          span(icon).style(Styles.FEATURE_ICON),
          span().style(Styles.FEATURE_TEXT).text(text));
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
