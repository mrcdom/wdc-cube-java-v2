package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.framework.vdom.SelUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;

public class RootView extends AbstractVDomView<RootPresenter> {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;

        String ROOT = clsx(u.FLEX_COL, u.H_FULL, u.OVERFLOW_HIDDEN);
        String CONTENT = clsx(u.FLEX_COL, u.FLEX_GROW, u.MIN_H_0);
    }

    private final RootViewState state;
    private boolean appended;

    public RootView(RootPresenter presenter) {
        super("root", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("sp-theme"));
        this.state = presenter.state;
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
        var contentElement = this.state.contentView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        // @formatter:off
        return spTheme("light", "medium", "spectrum")
          .cls(Sel.ROOT)
          .children(
            slot(contentElement)
              .cls(Sel.CONTENT));
        // @formatter:on
    }
}
