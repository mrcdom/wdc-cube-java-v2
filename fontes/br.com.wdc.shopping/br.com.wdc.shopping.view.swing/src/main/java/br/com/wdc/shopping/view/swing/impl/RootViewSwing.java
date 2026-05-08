package br.com.wdc.shopping.view.swing.impl;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.StackPanel;
import br.com.wdc.shopping.view.swing.util.SwingDom;
import br.com.wdc.shopping.view.swing.util.SwingUtils;

public class RootViewSwing extends AbstractViewSwing<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private StackPanel contentPane;
    private AbstractViewSwing<?> currentContentView;

    public RootViewSwing(RootPresenter presenter) {
        super("root", (ShoppingSwingApplication) presenter.app, presenter, new JPanel());
        this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.contentPane = null;
        this.currentContentView = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewSwing<?> v ? v : null;
        if (this.currentContentView != newContentView) {
        	SwingUtils.replaceContent(this.contentPane,
                    newContentView != null ? newContentView.getElement() : null);
            this.currentContentView = newContentView;
        }
    }

    private void initialRender(SwingDom dom, JPanel pane0) {
        dom.stackPane(content -> this.contentPane = (StackPanel) content);
        SwingUtils.replaceContent(this.app.getRootPane(), pane0);
    }
}
