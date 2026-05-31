package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class RootView extends AbstractVDomView<RootPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Sel {

        String ROOT = "flex-col h-full overflow-hidden";
        String CONTENT = "flex-col flex-grow min-h-0";

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
