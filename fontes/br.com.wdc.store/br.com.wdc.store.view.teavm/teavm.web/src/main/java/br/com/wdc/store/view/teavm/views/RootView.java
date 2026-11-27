package br.com.wdc.store.view.teavm.views;

import static br.com.wdc.store.view.teavm.commons.Swc.*;
import static br.com.wdc.store.view.teavm.commons.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelComponents;
import br.com.wdc.store.view.teavm.commons.SelIcons;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.VNode;

public class RootView extends AbstractVDomView<RootScope> {

    @SuppressWarnings({"java:S1214", "static-access"})
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

    private boolean appended;

    public RootView(StoreTeaVMApplication app, RootScope scope) {
        super("root", app, scope,
                HTMLDocument.current().createElement("sp-theme"));
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        if (!this.appended) {
            HTMLDocument.current().getBody().appendChild(this.element);
            this.appended = true;
        }
    }

    @Override
    protected VNode render() {
        var contentElement = this.viewOf(this.scope.content.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        var errorMessage = I18n.text(this.scope.errorMessage.get());
        // @formatter:off
        return spTheme("light", "medium", "spectrum")
          .cls(Sel.ROOT)
          .children(
            div(errorMessage.isEmpty() ? Sel.HIDDEN : Sel.ERROR).children(
              span(Sel.ERROR_ICON),
              span(Sel.ERROR_TEXT).text(errorMessage)),
            slot(contentElement)
              .cls(Sel.CONTENT));
        // @formatter:on
    }
}
