package br.com.wdc.store.view.teavm.views;

import static br.com.wdc.store.view.teavm.commons.Swc.*;
import static br.com.wdc.store.view.teavm.commons.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.VNode;

public class RootView extends AbstractVDomView<RootScope> {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;

        String ROOT = clsx(u.FLEX_COL, u.H_FULL, u.OVERFLOW_HIDDEN);
        String CONTENT = clsx(u.FLEX_COL, u.FLEX_GROW, u.MIN_H_0);
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
        var contentElement = this.app.viewOf(this.scope.content.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        // @formatter:off
        return spTheme("light", "medium", "spectrum")
          .cls(Sel.ROOT)
          .children(
            slot(contentElement)
              .cls(Sel.CONTENT));
        // @formatter:on
    }
}
