package br.com.wdc.shopping.view.robovm.impl;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIButtonType;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.NSTextAlignment;

import java.text.SimpleDateFormat;
import java.util.Locale;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class PurchasesPanelViewRoboVM extends AbstractViewRoboVM<PurchasesPanelPresenter> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));

    private final PurchasesPanelViewState state;
    private boolean built;

    private UIScrollView scrollView;
    private UILabel pageLabel;

    public PurchasesPanelViewRoboVM(ShoppingRoboVMApplication app, PurchasesPanelPresenter presenter) {
        super("purchases-panel", app, presenter);
        this.state = presenter.state;
    }

    private void buildUI() {
        var container = new UIView(new CGRect(0, 0, 375, 200));
        container.setBackgroundColor(UIColor.clear());

        // Section header - white bold over doodle background
        var title = new UILabel(new CGRect(20, 2, 340, 22));
        title.setText("COMPRAS RECENTES");
        title.setFont(UIFont.getBoldSystemFont(14));
        title.setTextColor(UIColor.white());
        title.setShadowColor(UIColor.fromRGBA(0.0, 0.0, 0.0, 0.3));
        title.setShadowOffset(new org.robovm.apple.coregraphics.CGSize(0, 1));
        container.addSubview(title);

        // White card for items
        scrollView = new UIScrollView(new CGRect(16, 26, 343, 130));
        scrollView.getLayer().setCornerRadius(10);
        scrollView.setClipsToBounds(true);
        scrollView.setBackgroundColor(UIColor.white());
        container.addSubview(scrollView);

        // Pagination
        var prevButton = new UIButton(UIButtonType.System);
        prevButton.setFrame(new CGRect(16, 162, 70, 36));
        prevButton.setTitle("‹ Ant", UIControlState.Normal);
        prevButton.getTitleLabel().setFont(UIFont.getSystemFont(15));
        prevButton.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
        prevButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("prevPage", () -> {
                    if (state.page > 0) {
                        presenter.onPageChange(state.page - 1);
                    }
                }));
        container.addSubview(prevButton);

        pageLabel = new UILabel(new CGRect(110, 162, 155, 36));
        pageLabel.setTextAlignment(NSTextAlignment.Center);
        pageLabel.setFont(UIFont.getSystemFont(13));
        pageLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        container.addSubview(pageLabel);

        var nextButton = new UIButton(UIButtonType.System);
        nextButton.setFrame(new CGRect(290, 162, 70, 36));
        nextButton.setTitle("Próx ›", UIControlState.Normal);
        nextButton.getTitleLabel().setFont(UIFont.getSystemFont(15));
        nextButton.setTitleColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0), UIControlState.Normal);
        nextButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("nextPage", () -> {
                    int totalPages = (state.totalCount + state.pageSize - 1) / state.pageSize;
                    if (state.page < totalPages - 1) {
                        presenter.onPageChange(state.page + 1);
                    }
                }));
        container.addSubview(nextButton);

        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
        }

        // Clear and rebuild purchases list
        for (var sub : scrollView.getSubviews()) {
            sub.removeFromSuperview();
        }

        if (state.purchases != null) {
            int yOffset = 0;
            int count = state.purchases.size();
            for (int i = 0; i < count; i++) {
                var purchase = state.purchases.get(i);
                var row = new UIButton(UIButtonType.Custom);
                row.setFrame(new CGRect(0, yOffset, 343, 44));

                var dateLabel = new UILabel(new CGRect(16, 4, 110, 18));
                dateLabel.setText(purchase.date > 0 ? DATE_FORMAT.format(new java.util.Date(purchase.date)) : "");
                dateLabel.setFont(UIFont.getSystemFont(15));
                dateLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                dateLabel.setUserInteractionEnabled(false);
                row.addSubview(dateLabel);

                var totalLabel = new UILabel(new CGRect(130, 4, 120, 18));
                totalLabel.setText(String.format("R$ %.2f", purchase.total));
                totalLabel.setFont(UIFont.getSystemFont(17));
                totalLabel.setTextAlignment(NSTextAlignment.Right);
                totalLabel.setUserInteractionEnabled(false);
                row.addSubview(totalLabel);

                // Chevron
                var chevron = new UILabel(new CGRect(315, 10, 20, 24));
                chevron.setText("›");
                chevron.setFont(UIFont.getSystemFont(20));
                chevron.setTextColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
                chevron.setUserInteractionEnabled(false);
                row.addSubview(chevron);

                final var purchaseId = purchase.id;
                row.addOnTouchUpInsideListener((c, e) ->
                        safeAction("openReceipt", () -> presenter.onOpenReceipt(purchaseId)));

                // Inset separator
                if (i < count - 1) {
                    var separator = new UIView(new CGRect(16, 43.5, 327, 0.5));
                    separator.setBackgroundColor(UIColor.fromRGBA(0.78, 0.78, 0.80, 1.0));
                    separator.setUserInteractionEnabled(false);
                    row.addSubview(separator);
                }

                scrollView.addSubview(row);
                yOffset += 44;
            }
            scrollView.setContentSize(new org.robovm.apple.coregraphics.CGSize(343, yOffset));
        }

        // Update pagination
        int totalPages = state.pageSize > 0 ? (state.totalCount + state.pageSize - 1) / state.pageSize : 0;
        pageLabel.setText(String.format("Página %d de %d", state.page + 1, Math.max(1, totalPages)));
    }
}
