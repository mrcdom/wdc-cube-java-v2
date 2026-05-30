package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.slot;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewStateCoordinator;

/**
 * Browser-level view: manages global submitting indicator and hosts the root view.
 * Equivalent to BrowserView.tsx in remote.shell.react.
 */
public class BrowserView extends AbstractRemoteView {

    public static final String VIEW_ID = ViewStateCoordinator.BROWSER_VID;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String HOST = "browser-host";
        String CONTAINER = "browser-container";
        String LOADING_BAR = "browser-loading-bar";
        String HIDDEN = CssUtility.HIDDEN;
        String CONTENT = "browser-content";
    }

    private boolean appended;

    public BrowserView(String vsid) {
        super(vsid);
        getElement().setAttribute("class", Css.HOST);
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

        // @formatter:off
        return div(Css.CONTAINER).children(
          div(submitting ? Css.LOADING_BAR : Css.HIDDEN),
          slot(Css.CONTENT, contentEl));
        // @formatter:on
    }
}
