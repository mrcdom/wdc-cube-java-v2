package br.com.wdc.shopping.view.swing.impl;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.StackPanel;

public class RootViewSwing extends AbstractViewSwing<RootPresenter> {

    private final RootViewState state;

    private boolean notRendered = true;
    private StackPanel contentPane;
    private AbstractViewSwing<?> currentContentView;

    public RootViewSwing(ShoppingSwingApplication app, RootPresenter presenter) {
        super("root", app, presenter, new JPanel());
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
            this.contentPane = new StackPanel();
            this.element.add(this.contentPane);
            this.app.getRootPane().removeAll();
            this.app.getRootPane().add(this.element);
            this.app.getRootPane().revalidate();
            this.app.getRootPane().repaint();
            this.notRendered = false;
        }

        var newContentView = this.state.contentView instanceof AbstractViewSwing<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.removeAll();
            if (newContentView != null) {
                this.contentPane.add(newContentView.getElement());
            }
            this.currentContentView = newContentView;
            this.contentPane.revalidate();
            this.contentPane.repaint();
        }
    }
}
