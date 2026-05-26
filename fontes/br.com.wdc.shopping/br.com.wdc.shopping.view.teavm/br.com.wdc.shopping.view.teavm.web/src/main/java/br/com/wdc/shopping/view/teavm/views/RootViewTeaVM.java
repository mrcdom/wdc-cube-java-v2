package br.com.wdc.shopping.view.teavm.views;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class RootViewTeaVM extends AbstractViewTeaVM<RootPresenter> {

    private final RootViewState state;
    private HTMLElement contentSlot;
    private AbstractViewTeaVM<?> currentContentView;

    public RootViewTeaVM(RootPresenter presenter) {
        super("root", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "flex-column");
        this.element.setAttribute("style", "height:100%;overflow:hidden");
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            HTMLDocument.current().getBody().appendChild(this.element);
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewTeaVM<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentSlot.clear();
            if (newContentView != null) {
                this.contentSlot.appendChild(newContentView.getElement());
            }
            this.currentContentView = newContentView;
        }
    }

    private void buildUI(HtmlDom dom, HTMLElement root) {
        this.contentSlot = dom.div("flex-grow-1 d-flex flex-column", slot -> {
            slot.setAttribute("style", "min-height:0");
        });
    }
}
