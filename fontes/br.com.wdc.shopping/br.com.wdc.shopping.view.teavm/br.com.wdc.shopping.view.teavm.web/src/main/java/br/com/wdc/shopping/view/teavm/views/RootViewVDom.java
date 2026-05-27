package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

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
        super("root", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "flex-column");
        this.element.setAttribute("style", "height:100%;overflow:hidden");
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
        return slot("flex-grow-1 d-flex flex-column", contentElement)
                .style("min-height:0");
    }
}
