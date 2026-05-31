package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.slot;

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

        // @formatter:off
        return div(Sel.CONTAINER).children(
          div(submitting ? Sel.LOADING_BAR : Sel.HIDDEN),
          slot(Sel.CONTENT, contentEl));
        // @formatter:on
    }
}
