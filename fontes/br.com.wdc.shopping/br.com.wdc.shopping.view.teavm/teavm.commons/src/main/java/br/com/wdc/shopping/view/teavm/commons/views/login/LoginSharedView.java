package br.com.wdc.shopping.view.teavm.commons.views.login;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spFieldLabel;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spTextField;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;
import static br.com.wdc.shopping.view.teavm.commons.VNode.textNode;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;

/**
 * Shared Login view implementation using Virtual DOM + Spectrum Web Components.
 * <p>
 * This view is agnostic to the hosting mode (local presenter or remote bridge).
 * State and actions are injected via lambdas:
 * <ul>
 *   <li>{@link #stateSupplier} — provides the current {@link LoginViewState}</li>
 *   <li>{@link #onEnter} — action listener: receives (userName, password)</li>
 * </ul>
 */
public class LoginSharedView extends SharedVDomView {

    public static final String VIEW_ID = "c677cda52d14";

    // -- External bindings (set by adapter) --

    /** Provides the current view state. */
    public Supplier<LoginViewState> stateSupplier;

    /** Action: user requests login. Receives (userName, password). */
    public BiConsumer<String, String> onEnter;

    // -- Sel constants --

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String LOGIN_ROOT = "login-root";
        String LEFT_PANEL = clsx(u.MD_SHOW, "login-left-panel");
        String DECO_CIRCLE_1 = c.DECO_CIRCLE_1;
        String DECO_CIRCLE_2 = c.DECO_CIRCLE_2;
        String DECO_CIRCLE_3 = c.DECO_CIRCLE_3;
        String CONTENT_CENTER = "login-content-center";
        String LOGO_BOX_LG = clsx(c.LOGO_BOX_LG, u.MB_24);
        String ICON_LG = clsx(icon.BAG_CHECK, "login-logo-icon-lg");
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
        String ICON_SM = clsx(icon.BAG_CHECK, "login-icon-sm");
        String MOBILE_TITLE = "login-mobile-title";
        String MOBILE_SUBTITLE = "login-mobile-subtitle";
        String WELCOME_WRAP = "login-welcome-wrap";
        String WELCOME_TITLE = "login-welcome-title";
        String WELCOME_SUBTITLE = "login-welcome-subtitle";
        String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_16);
        String HIDDEN = u.HIDDEN;
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
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

    // -- DOM refs --

    private HTMLInputElement userNameField;
    @SuppressWarnings("java:S2068")
    private HTMLInputElement passwordField;

    // -- Stable event listeners --

    private final EventListener<KeyboardEvent> onKeyDown = evt -> {
        if ("Enter".equals(evt.getKey())) {
            emitEnter();
        }
    };
    private final EventListener<Event> onClickEnter = evt -> emitEnter();

    // -- Render --

    @Override
    public VNode render() {
        var state = stateSupplier.get();
        var loading = state.loading;
        var errorMessage = state.errorMessage;
        var showError = errorMessage != null && !errorMessage.isEmpty();
        
        var title = "WDC Shopping";
        var subtitle = "Sua compra certa na internet.";

        // @formatter:off
        return div(Sel.LOGIN_ROOT).children(
          // Left decorative panel (hidden on mobile)
          div(Sel.LEFT_PANEL).children(
            div(Sel.DECO_CIRCLE_1),
            div(Sel.DECO_CIRCLE_2),
            div(Sel.DECO_CIRCLE_3),
            div(Sel.CONTENT_CENTER).children(
              div(Sel.LOGO_BOX_LG)
                .children(span(Sel.ICON_LG)),
              div(Sel.TITLE_LG).children(textNode(title)),
              div(Sel.SUBTITLE_LG).children(textNode(subtitle)),
              div(Sel.FEATURES_LIST).children(
                renderFeature(SelIcons.SHIELD_CHECK, "Compra segura"),
                renderFeature(SelIcons.TRUCK, "Entrega rápida"),
                renderFeature(SelIcons.ARROW_REPEAT, "Troca garantida")))),
          // Right: form panel
          div(Sel.FORM_PANEL).children(
            div(Sel.FORM_CONTENT).children(
              // Mobile-only logo
              div(Sel.MOBILE_LOGO_BG).children(
                div(Sel.MOBILE_CIRCLE_1),
                div(Sel.MOBILE_CIRCLE_2),
                div(Sel.MOBILE_CONTENT).children(
                  div(Sel.LOGO_BOX_SM)
                    .children(span(Sel.ICON_SM)),
                  div(Sel.MOBILE_TITLE).children(textNode(title)),
                  div(Sel.MOBILE_SUBTITLE).children(textNode(subtitle)))),
              // Welcome text
              div(Sel.WELCOME_WRAP).children(
                div(Sel.WELCOME_TITLE).children(textNode("Bem-vindo")),
                div(Sel.WELCOME_SUBTITLE).children(textNode("Entre com suas credenciais para continuar"))),
              // Error alert
              div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
                span(Sel.ERROR_ICON),
                span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // User field
              spFieldLabel("login-user", "Usuário").cls(Sel.FIELD_LABEL),
              spTextField("Digite seu usuário")
                .attr("id", "login-user")
                .attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .cls(Sel.USER_FIELD)
                .ref(el -> this.userNameField = (HTMLInputElement) el),
              // Password field
              spFieldLabel("login-pass", "Senha").cls(Sel.FIELD_LABEL),
              spTextField("Digite sua senha", "password")
                .attr("id", "login-pass")
                .attr("autocomplete", "off")
                .boolAttr("disabled", loading)
                .cls(Sel.PASSWORD_FIELD)
                .on("keydown", onKeyDown)
                .ref(el -> this.passwordField = (HTMLInputElement) el),
              // Enter button
              spButton("accent", "l")
                .boolAttr("disabled", loading)
                .boolAttr("pending", loading)
                .cls(Sel.ENTER_BUTTON)
                .on("click", onClickEnter)
                .children(textNode(loading ? "Entrando..." : "Entrar")),
              // Demo hint
              div(Sel.DEMO_HINT).children(
                span(Sel.DEMO_TEXT).text("Acesso demo: "),
                span(Sel.DEMO_HIGHLIGHT).text("admin"),
                span(Sel.DEMO_TEXT).text(" / "),
                span(Sel.DEMO_HIGHLIGHT).text("admin")))));
        // @formatter:on
    }

    private VNode renderFeature(String icon, String text) {
        // @formatter:off
        return div(Sel.FEATURE_ROW).children(
          span(clsx(icon, Sel.FEATURE_ICON)),
          span(Sel.FEATURE_TEXT).text(text));
        // @formatter:on
    }

    private void emitEnter() {
        String userName = userNameField != null ? userNameField.getValue() : "";
        String password = passwordField != null ? passwordField.getValue() : "";
        onEnter.accept(userName, password);
    }
}
