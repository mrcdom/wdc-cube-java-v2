package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.Swc.spFieldLabel;
import static br.com.wdc.framework.vdom.Swc.spTextField;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.span;
import static br.com.wdc.framework.vdom.VNode.textNode;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.DataSecurity;

/**
 * Login view with Spectrum Web Components + VDom. Reads state from server: userName, password, errorMessage, loading.
 */
public class LoginView extends AbstractRemoteView {

    public static final String VIEW_ID = "c677cda52d14";

    private static final int ON_ENTER = 1;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String LOGIN_ROOT = "login-root";
        String LEFT_PANEL = clsx(CssUtility.MD_SHOW, "login-left-panel");
        String DECO_CIRCLE_1 = "deco-circle--1";
        String DECO_CIRCLE_2 = "deco-circle--2";
        String DECO_CIRCLE_3 = "deco-circle--3";
        String CONTENT_CENTER = "login-content-center";
        String LOGO_BOX_LG = "logo-box-lg";
        String ICON_LG = clsx(CssIcons.BAG_CHECK, "login-logo-icon-lg");
        String TITLE_LG = "login-title-lg";
        String SUBTITLE_LG = "login-subtitle-lg";
        String FEATURES_LIST = "login-features-list";
        String FORM_PANEL = "login-card login-form-panel";
        String FORM_CONTENT = "login-form-content";
        String MOBILE_LOGO_BG = "login-mobile-logo";
        String MOBILE_CIRCLE_1 = "login-mobile-circle-1";
        String MOBILE_CIRCLE_2 = "login-mobile-circle-2";
        String MOBILE_CONTENT = "login-mobile-content";
        String LOGO_BOX_SM = "login-logo-box-sm";
        String ICON_SM = clsx(CssIcons.BAG_CHECK, "login-icon-sm");
        String MOBILE_TITLE = "login-mobile-title";
        String MOBILE_SUBTITLE = "login-mobile-subtitle";
        String WELCOME_WRAP = "login-welcome-wrap";
        String WELCOME_TITLE = "login-welcome-title";
        String WELCOME_SUBTITLE = "login-welcome-subtitle";
        String ERROR_VISIBLE = CssComponents.ALERT_ERROR;
        String HIDDEN = CssUtility.HIDDEN;
        String ERROR_ICON = clsx(CssIcons.EXCLAMATION_CIRCLE, CssComponents.ALERT_ERROR_ICON);
        String ERROR_TEXT = CssComponents.ALERT_ERROR_TEXT;
        String FIELD_LABEL = "login-field-label";
        String USER_FIELD = "login-field";
        String PASSWORD_FIELD = "login-field-password";
        String ENTER_BUTTON = "login-enter-btn";
        String DEMO_HINT = "login-demo-hint";
        String DEMO_TEXT = "login-demo-text";
        String DEMO_HIGHLIGHT = "login-demo-highlight";
        String FEATURE_ROW = "login-feature-row";
        String FEATURE_ICON = "login-feature-icon";
        String FEATURE_TEXT = "login-feature-text";
    }

    private HTMLInputElement userNameField;
    @SuppressWarnings("java:S2068")
    private HTMLInputElement passwordField;

    public LoginView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        boolean loading = scope.getBoolean("loading");
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        // @formatter:off
        return div(Css.LOGIN_ROOT).children(
          // Left decorative panel (hidden on mobile)
          div(Css.LEFT_PANEL).children(
            div(Css.DECO_CIRCLE_1),
            div(Css.DECO_CIRCLE_2),
            div(Css.DECO_CIRCLE_3),
            div(Css.CONTENT_CENTER).children(
              div(Css.LOGO_BOX_LG).children(
                span(Css.ICON_LG)),
              div(Css.TITLE_LG).children(textNode("WDC Shopping")),
              div(Css.SUBTITLE_LG).children(textNode("Sua compra certa na internet.")),
              div(Css.FEATURES_LIST).children(
                renderFeature(CssIcons.SHIELD_CHECK, "Compra segura"),
                renderFeature(CssIcons.TRUCK, "Entrega rápida"),
                renderFeature(CssIcons.ARROW_REPEAT, "Troca garantida")))),
          // Right: form panel
          div(Css.FORM_PANEL).children(
            div(Css.FORM_CONTENT).children(
              // Mobile-only logo
              div(Css.MOBILE_LOGO_BG).children(
                div(Css.MOBILE_CIRCLE_1),
                div(Css.MOBILE_CIRCLE_2),
                div(Css.MOBILE_CONTENT).children(
                  div(Css.LOGO_BOX_SM).children(
                    span(Css.ICON_SM)),
                  div(Css.MOBILE_TITLE).children(textNode("WDC Shopping")),
                  div(Css.MOBILE_SUBTITLE).children(textNode("Sua compra certa na internet.")))),
              // Welcome text
              div(Css.WELCOME_WRAP).children(
                div(Css.WELCOME_TITLE).children(textNode("Bem-vindo")),
                div(Css.WELCOME_SUBTITLE).children(textNode("Entre com suas credenciais para continuar"))),
              // Error alert
              div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
                span(Css.ERROR_ICON),
                span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // User field
              spFieldLabel("login-user", "Usuário").cls(Css.FIELD_LABEL),
              spTextField("Digite seu usuário")
                .attr("id", "login-user").attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .cls(Css.USER_FIELD)
                .ref(el -> this.userNameField = (HTMLInputElement) el),
              // Password field
              spFieldLabel("login-pass", "Senha").cls(Css.FIELD_LABEL),
              spTextField("Digite sua senha", "password")
                .attr("id", "login-pass").attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .cls(Css.PASSWORD_FIELD)
                .on("keydown", (KeyboardEvent evt) -> { if ("Enter".equals(evt.getKey())) { emitEnter(); } })
                .ref(el -> this.passwordField = (HTMLInputElement) el),
              // Enter button
              spButton("accent", "l")
                .boolAttr("disabled", loading).boolAttr("pending", loading)
                .cls(Css.ENTER_BUTTON)
                .on("click", evt -> emitEnter())
                .children(textNode(loading ? "Entrando..." : "Entrar")),
              // Demo hint
              div(Css.DEMO_HINT).children(
                span(Css.DEMO_TEXT).text("Acesso demo: "),
                span(Css.DEMO_HIGHLIGHT).text("admin"),
                span(Css.DEMO_TEXT).text(" / "),
                span(Css.DEMO_HIGHLIGHT).text("admin")))));
        // @formatter:on
    }

    private VNode renderFeature(String icon, String text) {
        // @formatter:off
        return div(Css.FEATURE_ROW).children(
          span(clsx(icon, Css.FEATURE_ICON)),
          span(Css.FEATURE_TEXT).text(text));
        // @formatter:on
    }

    private void emitEnter() {
        String userName = userNameField != null ? userNameField.getValue() : "";
        String password = passwordField != null ? passwordField.getValue() : "";
        setFormField("userName", userName);
        DataSecurity.cipher(password, encryptedPassword -> {
            setFormField("password", encryptedPassword);
            submit(ON_ENTER);
        });
    }
}
