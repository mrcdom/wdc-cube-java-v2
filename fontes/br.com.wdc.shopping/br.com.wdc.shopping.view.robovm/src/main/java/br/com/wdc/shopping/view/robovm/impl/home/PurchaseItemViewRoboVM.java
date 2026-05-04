package br.com.wdc.shopping.view.robovm.impl.home;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIView;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class PurchaseItemViewRoboVM extends AbstractViewRoboVM<PurchasesPanelPresenter> {

    @SuppressWarnings("deprecation")
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    private PurchaseInfo state;

    private boolean notRendered = true;
    private UILabel dateLabel;
    private long dateOldValue;
    private UILabel totalLabel;
    private double totalOldValue;

    public PurchaseItemViewRoboVM(ShoppingRoboVMApplication app, PurchasesPanelPresenter presenter, int idx) {
        super("purchase-item-" + idx, app, presenter);
    }

    public void setState(PurchaseInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.dateLabel = null;
        this.dateOldValue = 0;
        this.totalLabel = null;
        this.totalOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.rootView = new UIView(new CGRect(0, 0, 343, 44));
            UIKitDom.render(this.rootView, this::initialRender);
            this.notRendered = false;
        }

        if (this.dateOldValue != this.state.date) {
            dateLabel.setText(this.state.date > 0 ? DATE_FORMAT.format(new java.util.Date(this.state.date)) : "");
            this.dateOldValue = this.state.date;
        }

        if (this.totalOldValue != this.state.total) {
            totalLabel.setText(String.format("R$ %.2f", this.state.total));
            this.totalOldValue = this.state.total;
        }
    }

    @SuppressWarnings("unused")
    private void initialRender(UIKitDom dom, UIView root) {
        var rowButton = new org.robovm.apple.uikit.UIButton(UIButtonType.Custom);
        rowButton.setFrame(new CGRect(0, 0, 343, 44));

        dom.label(110, 18, date -> {
            this.dateLabel = date;
            date.setFrame(new CGRect(16, 4, 110, 18));
            date.setText(this.state.date > 0 ? DATE_FORMAT.format(new java.util.Date(this.state.date)) : "");
            date.setFont(UIFont.getSystemFont(15));
            date.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
            date.setUserInteractionEnabled(false);
            this.dateOldValue = this.state.date;
        });

        dom.label(120, 18, total -> {
            this.totalLabel = total;
            total.setFrame(new CGRect(130, 4, 120, 18));
            total.setText(String.format("R$ %.2f", this.state.total));
            total.setFont(UIFont.getSystemFont(17));
            total.setTextAlignment(NSTextAlignment.Right);
            total.setUserInteractionEnabled(false);
            this.totalOldValue = this.state.total;
        });

        // Chevron
        dom.label(20, 24, chevron -> {
            chevron.setFrame(new CGRect(315, 10, 20, 24));
            chevron.setText("›");
            chevron.setFont(UIFont.getSystemFont(20));
            chevron.setTextColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
            chevron.setUserInteractionEnabled(false);
        });

        final var purchaseId = this.state.id;
        rowButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("openReceipt", () -> presenter.onOpenReceipt(purchaseId)));

        // Move subviews into the button
        for (var sub : root.getSubviews()) {
            sub.removeFromSuperview();
            rowButton.addSubview(sub);
        }
        root.addSubview(rowButton);
    }
}
