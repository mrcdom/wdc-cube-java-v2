package br.com.wdc.store.view.remote.shell.teavm.views;

import static br.com.wdc.store.view.teavm.commons.Swc.spTheme;
import static br.com.wdc.store.view.teavm.commons.VNode.clsx;
import static br.com.wdc.store.view.teavm.commons.VNode.div;
import static br.com.wdc.store.view.teavm.commons.VNode.slot;
import static br.com.wdc.store.view.teavm.commons.VNode.span;

import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelComponents;
import br.com.wdc.store.view.teavm.commons.SelIcons;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.VNode;

/**
 * Root view: hosts the current content view (login or home) and shows the unexpected-error message. Equivalent to RootView in remote.shell.react and
 * RootViewVDom in teavm.web.
 */
public class RootView extends AbstractRemoteView {

    public static final String VIEW_ID = RootScope.CLASS_ID;

    @SuppressWarnings({ "java:S1214", "static-access" })
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = clsx(u.FLEX_COL, u.H_FULL, u.OVERFLOW_HIDDEN);
        String CONTENT = clsx(u.FLEX_COL, u.FLEX_GROW, u.MIN_H_0);
        String HIDDEN = u.HIDDEN;
        String ERROR = c.ALERT_ERROR;
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
    }

    public RootView(String vsid) {
        super(vsid, "sp-theme");
        getElement().setAttribute("color", "light");
        getElement().setAttribute("scale", "medium");
        getElement().setAttribute("system", "spectrum");
        getElement().setAttribute("class", Sel.ROOT);
    }

    @Override
    protected VNode render() {
        var scope = scope();
        var contentVsid = scope.getString("contentViewId");
        var contentEl = getChildViewElement(contentVsid);
        var errorMessage = I18n.text(getMessage("errorMessage"));

        // @formatter:off
        return spTheme("light", "medium", "spectrum").cls(Sel.ROOT).children(
          div(errorMessage.isEmpty() ? Sel.HIDDEN : Sel.ERROR).children(
            span(Sel.ERROR_ICON),
            span(Sel.ERROR_TEXT).text(errorMessage)),
          slot(contentEl).cls(Sel.CONTENT));
        // @formatter:on
    }
}
