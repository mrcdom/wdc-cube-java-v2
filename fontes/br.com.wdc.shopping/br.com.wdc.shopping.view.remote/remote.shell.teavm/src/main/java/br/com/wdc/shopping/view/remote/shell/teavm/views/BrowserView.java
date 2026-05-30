package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewStateCoordinator;
import br.com.wdc.framework.vdom.VNode;

/**
 * Browser-level view: manages global submitting indicator and hosts the root view.
 * Equivalent to BrowserView.tsx in remote.shell.react.
 */
public class BrowserView extends AbstractRemoteView {

    public static final String VIEW_ID = ViewStateCoordinator.BROWSER_VID;

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String HOST = css()
                .flexCol()
                .height("100%")
                .overflowHidden()
                .build();

        String CONTAINER = css()
                .flexCol()
                .flex("1")
                .height("100%")
                .minHeight("0")
                .build();

        String LOADING_BAR = css()
                .position("fixed")
                .top("0")
                .left("0")
                .right("0")
                .height("3px")
                .background("var(--app-accent)")
                .zIndex(9999)
                .animation("progress 1.5s ease-in-out infinite")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String CONTENT = css()
                .flexCol()
                .flex("1")
                .minHeight("0")
                .build();
    }

    private boolean appended;

    public BrowserView(String vsid) {
        super(vsid);
        getElement().setAttribute("style", Styles.HOST);
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
        return div().style(Styles.CONTAINER).children(
          div().style(submitting ? Styles.LOADING_BAR : Styles.HIDDEN),
          slot(contentEl).style(Styles.CONTENT));
        // @formatter:on
    }
}
