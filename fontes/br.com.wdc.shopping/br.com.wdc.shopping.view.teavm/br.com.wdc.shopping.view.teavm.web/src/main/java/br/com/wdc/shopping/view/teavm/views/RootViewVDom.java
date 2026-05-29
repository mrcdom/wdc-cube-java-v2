package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.Swc.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class RootViewVDom extends AbstractVDomView<RootPresenter> {

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
                    .style("display:flex;flex-direction:column;height:100%;overflow:hidden")
                    .children(
                        slot("", contentElement)
                            .style("display:flex;flex-direction:column;flex-grow:1;min-height:0"));
        // @formatter:on
    }
}
