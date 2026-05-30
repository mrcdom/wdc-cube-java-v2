package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spTheme;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.slot;

import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Root view: hosts the current content view (login or home).
 * Equivalent to RootView in remote.shell.react and RootViewVDom in teavm.web.
 */
public class RootView extends AbstractRemoteView {

    public static final String VIEW_ID = "f2d345c4a610";

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = clsx(CssUtility.FLEX_COL, CssUtility.H_FULL, CssUtility.OVERFLOW_HIDDEN);
        String CONTENT = clsx(CssUtility.FLEX_COL, CssUtility.FLEX_GROW, CssUtility.MIN_H_0);
    }

    public RootView(String vsid) {
        super(vsid, "sp-theme");
        getElement().setAttribute("color", "light");
        getElement().setAttribute("scale", "medium");
        getElement().setAttribute("system", "spectrum");
        getElement().setAttribute("class", Css.ROOT);
    }

    @Override
    protected VNode render() {
        var scope = state();
        var contentVsid = scope.getString("contentViewId");
        var contentEl = getChildViewElement(contentVsid);

        // @formatter:off
        return spTheme("light", "medium", "spectrum").cls(Css.ROOT).children(
          slot(contentEl).cls(Css.CONTENT));
        // @formatter:on
    }
}
