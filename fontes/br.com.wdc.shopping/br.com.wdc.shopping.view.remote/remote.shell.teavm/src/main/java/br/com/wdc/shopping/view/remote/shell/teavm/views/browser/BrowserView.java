package br.com.wdc.shopping.view.remote.shell.teavm.views.browser;

import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.slot;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewStateCoordinator;

/**
 * Browser-level view: manages global submitting indicator and hosts the root view. Equivalent to BrowserView.tsx in
 * remote.shell.react.
 */
public class BrowserView extends AbstractRemoteView {

    public static final String VIEW_ID = ViewStateCoordinator.BROWSER_VID;

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
        var scope = state();
        var contentVsid = scope.getString("contentViewId");
        var contentEl = getChildViewElement(contentVsid);
        boolean submitting = ViewStateCoordinator.INSTANCE.isSubmitting()
                || scope.getBoolean("submitting");

        var errorCause = scope.getString("error.cause");

        // @formatter:off
        return div(Sel.CONTAINER).children(
          div(submitting ? Sel.LOADING_BAR : Sel.HIDDEN),
          errorCause != null ? renderConnectionAlert(scope.getInt("error.delay")) : null,
          slot(Sel.CONTENT, contentEl));
        // @formatter:on
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
                    ? "Conectando em " + minutes + "m e " + seconds + "s..."
                    : "Conectando em " + seconds + "s...";
            showRetry = true;
        } else {
            timeText = "Conectando agora...";
            showRetry = false;
        }

        var alert = div("alert-error")
                .style("display:inline-flex;border-radius:0 0 8px 8px;padding:6px 12px;margin:0")
                .children(
                        span("font-bold text-sm").style("color:var(--app-error-text)").text("Não conectado. "),
                        span("text-sm").style("color:var(--app-error-text)").text(timeText));

        if (showRetry) {
            alert.children(
                    span("text-sm font-medium cursor-pointer ml-4")
                            .style("color:var(--app-accent);text-decoration:underline")
                            .text("Tentar agora")
                            .on("click", e -> ViewStateCoordinator.INSTANCE.reconnectController.checkNow()));
        }

        return div().style("text-align:center").children(alert);
    }
}
