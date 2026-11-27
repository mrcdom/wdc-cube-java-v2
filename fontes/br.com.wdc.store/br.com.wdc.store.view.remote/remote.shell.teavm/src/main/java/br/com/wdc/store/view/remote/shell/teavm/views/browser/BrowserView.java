package br.com.wdc.store.view.remote.shell.teavm.views.browser;

import static br.com.wdc.store.view.teavm.commons.VNode.div;
import static br.com.wdc.store.view.teavm.commons.VNode.slot;
import static br.com.wdc.store.view.teavm.commons.VNode.span;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.framework.cube.remote.bridge.teavm.ScopeCoordinator;
import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.VNode;

/**
 * Browser-level view: manages global submitting indicator and hosts the root view. Equivalent to BrowserView.tsx in
 * remote.shell.react.
 */
public class BrowserView extends AbstractRemoteView {

    public static final String VIEW_ID = ScopeCoordinator.BROWSER_VID;

    // :: Events (codes of the browser's actions on the host)
    private static final int ON_ALERT_OK = 1;

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel extends SelUtility {
        SelUtility u = SelUtility.INSTANCE;

        String HIDDEN = u.HIDDEN;
        String HOST = "browser-host";
        String CONTAINER = "browser-container";
        String LOADING_BAR = "browser-loading-bar";
        String CONTENT = "browser-content";
    }

    private boolean appended;

    public BrowserView(String vsid) {
        super(vsid);
        getElement().setAttribute("class", Sel.HOST);
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        if (!appended) {
            removeLoadingScreen();
            HTMLDocument.current().getBody().appendChild(getElement());
            appended = true;
        }
    }

    @JSBody(params = {}, script = "var el = document.getElementById('loading'); if (el) el.remove();")
    private static native void removeLoadingScreen();

    @Override
    protected VNode render() {
        var scope = scope();
        var contentVsid = scope.getString("contentViewId");
        var contentEl = getChildViewElement(contentVsid);
        boolean submitting = ScopeCoordinator.INSTANCE.isSubmitting()
                || scope.getBoolean("submitting");

        var errorCause = scope.getString("error.cause");
        boolean outdated = scope.getBoolean(ScopeCoordinator.OUTDATED_FIELD);

        // @formatter:off
        return div(Sel.CONTAINER).children(
          div(submitting ? Sel.LOADING_BAR : Sel.HIDDEN),
          outdated ? renderOutdatedAlert() : null,
          errorCause != null ? renderConnectionAlert(scope.getInt("error.delay")) : null,
          scope.getInt("alertId") != 0 ? renderAppAlert(scope.getInt("alertId"), alertArgs(scope.getValues().get("alertArgs"))) : null,
          slot(Sel.CONTENT, contentEl));
        // @formatter:on
    }

    /** The host's alert: shown until dismissed, which tells the host (its alert action may run then). */
    private VNode renderAppAlert(int code, List<String> args) {
        String message;
        String detail = null;
        switch (code) {
            case -1 -> {
                message = args.isEmpty() ? I18n.text(StoreMessages.ERROR_UNEXPECTED) : args.get(0);
                detail = args.size() > 1 ? args.get(1) : null;
            }
            case -2 -> {
                message = I18n.text(StoreMessages.ALERT_URI_NOT_ACCESSIBLE, "uri", args.isEmpty() ? "" : args.get(0));
                detail = args.size() > 1 ? args.get(1) : null;
            }
            default -> message = args.isEmpty() ? I18n.text(StoreMessages.ERROR_UNEXPECTED) : args.get(0);
        }

        // @formatter:off
        return div("alert-error").style("margin:16px").children(
          div().style("flex:1").children(
            div("font-bold text-sm").style("color:var(--app-error-text)").text(I18n.text(StoreMessages.ALERT_TITLE)),
            span("text-sm").style("color:var(--app-error-text)").text(message),
            detail != null ? div("text-xs mt-4").style("color:var(--app-error-text)").text(detail) : null),
          span("text-sm font-medium cursor-pointer ml-4")
            .style("color:var(--app-accent)")
            .text(I18n.text(StoreMessages.ALERT_OK))
            .on("click", useCallback("alertOk", e -> submit(ON_ALERT_OK))));
        // @formatter:on
    }

    /** The host refused this page's protocol version: it does not reconnect, and its OK reloads the page. */
    private VNode renderOutdatedAlert() {
        // @formatter:off
        return div("alert-error").style("margin:16px").children(
          div().style("flex:1").children(
            div("font-bold text-sm").style("color:var(--app-error-text)").text(I18n.text(StoreMessages.ALERT_TITLE)),
            span("text-sm").style("color:var(--app-error-text)").text(I18n.text(StoreMessages.CONNECTION_OUTDATED_PAGE))),
          span("text-sm font-medium cursor-pointer ml-4")
            .style("color:var(--app-accent)")
            .text(I18n.text(StoreMessages.ALERT_OK))
            .on("click", useCallback("outdatedOk", e -> reload())));
        // @formatter:on
    }

    @JSBody(params = {}, script = "location.reload();")
    private static native void reload();

    private static List<String> alertArgs(Object value) {
        var args = new ArrayList<String>();
        if (value instanceof List<?> list) {
            for (var item : list) {
                args.add(String.valueOf(item));
            }
        }
        return args;
    }

    private VNode renderConnectionAlert(int delay) {
        String timeText;
        boolean showRetry;
        if (delay > 0) {
            int seconds = delay / 1000;
            int minutes = 0;
            if (seconds > 60) {
                minutes = seconds / 60;
                seconds = seconds - minutes * 60;
            }
            timeText = minutes > 0
                    ? I18n.text(StoreMessages.CONNECTION_RECONNECTING_IN_MINUTES, "minutes", String.valueOf(minutes), "seconds",
                            String.valueOf(seconds))
                    : I18n.text(StoreMessages.CONNECTION_RECONNECTING_IN_SECONDS, "seconds", String.valueOf(seconds));
            showRetry = true;
        } else {
            timeText = I18n.text(StoreMessages.CONNECTION_RECONNECTING_NOW);
            showRetry = false;
        }

        var alert = div("alert-error")
                .style("display:inline-flex;border-radius:0 0 8px 8px;padding:6px 12px;margin:0")
                .children(
                        span("font-bold text-sm").style("color:var(--app-error-text)").text(I18n.text(StoreMessages.CONNECTION_NOT_CONNECTED) + " "),
                        span("text-sm").style("color:var(--app-error-text)").text(timeText));

        if (showRetry) {
            alert.children(
                    span("text-sm font-medium cursor-pointer ml-4")
                            .style("color:var(--app-accent);text-decoration:underline")
                            .text(I18n.text(StoreMessages.CONNECTION_RETRY_NOW))
                            .on("click", e -> ScopeCoordinator.INSTANCE.reconnectController.checkNow()));
        }

        return div().style("text-align:center").children(alert);
    }
}
