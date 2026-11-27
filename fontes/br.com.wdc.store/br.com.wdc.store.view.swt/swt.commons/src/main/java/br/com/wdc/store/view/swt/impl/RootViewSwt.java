package br.com.wdc.store.view.swt.impl;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.swt.AbstractViewSwt;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.util.I18n;
import br.com.wdc.store.view.swt.util.SlotComposite;

public class RootViewSwt extends AbstractViewSwt {

    public Supplier<RootScope> scopeSupplier;

    private boolean notRendered = true;
    private SlotComposite contentSlot;
    private AbstractViewSwt currentContentView;

    public RootViewSwt(SwtApp app) {
        super("root", app, new Composite(app.getShell(), SWT.NONE));
        this.element.setLayout(new FillLayout());

        // Register this as the root pane in the application
        this.app.setRootPane(this.element);

        // Set as topControl of the Shell's StackLayout
        var shell = this.app.getShell();
        ((StackLayout) shell.getLayout()).topControl = this.element;
        shell.layout(true, true);
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            initialRender();
            this.notRendered = false;
        }

        var scope = this.scopeSupplier != null ? this.scopeSupplier.get() : null;
        var newContentView = scope != null && this.viewOf(scope.content.get()) instanceof AbstractViewSwt v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentSlot.setContent(newContentView != null ? newContentView.getElement() : null);
            this.currentContentView = newContentView;
            this.app.getShell().layout(true, true);
        }

        // An unexpected error: shown once, in a dialog over the content
        var error = scope != null ? scope.errorMessage.get() : null;
        if (error != null) {
            scope.errorMessage.setSilently(null);
            var shell = this.app.getShell();
            shell.getDisplay().asyncExec(() -> {
                if (shell.isDisposed()) {
                    return;
                }
                var box = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                box.setText(I18n.text(StoreMessages.ALERT_TITLE));
                box.setMessage(I18n.text(error));
                box.open();
            });
        }
    }

    private void initialRender() {
        this.contentSlot = new SlotComposite(this.element, this.app.getOffscreen());
        this.element.layout(true, true);
    }
}
