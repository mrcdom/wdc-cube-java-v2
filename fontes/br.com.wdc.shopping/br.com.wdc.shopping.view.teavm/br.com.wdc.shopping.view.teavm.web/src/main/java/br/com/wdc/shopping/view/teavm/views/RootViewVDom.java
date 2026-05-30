package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class RootViewVDom extends AbstractVDomView<RootPresenter> {

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

    private final RootViewState state;
    private boolean appended;

    public RootViewVDom(RootPresenter presenter) {
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
          .style(Styles.ROOT)
          .children(
            slot(contentElement)
              .style(Styles.CONTENT));
        // @formatter:on
    }
}
