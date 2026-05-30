package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.vdom.VNode;

/**
 * Root view: hosts the current content view (login or home).
 * Equivalent to RootView in remote.shell.react and RootViewVDom in teavm.web.
 */
public class RootView extends AbstractRemoteView {

    public static final String VIEW_ID = "f2d345c4a610";

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String ROOT = css()
                .flexCol()
                .height("100%")
                .overflowHidden()
                .build();

        String CONTENT = css()
                .flexCol()
                .flexGrow(1)
                .minHeight("0")
                .build();
    }

    public RootView(String vsid) {
        super(vsid, "sp-theme");
        getElement().setAttribute("color", "light");
        getElement().setAttribute("scale", "medium");
        getElement().setAttribute("system", "spectrum");
        getElement().setAttribute("style", Styles.ROOT);
    }

    @Override
    protected VNode render() {
        var scope = state();
        var contentVsid = scope.getString("contentViewId");
        var contentEl = getChildViewElement(contentVsid);

        // @formatter:off
        return spTheme("light", "medium", "spectrum").style(Styles.ROOT).children(
          slot(contentEl).style(Styles.CONTENT));
        // @formatter:on
    }
}
