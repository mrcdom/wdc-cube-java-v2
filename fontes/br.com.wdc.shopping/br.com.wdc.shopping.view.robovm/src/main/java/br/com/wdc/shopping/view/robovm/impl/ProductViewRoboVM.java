package br.com.wdc.shopping.view.robovm.impl;

import java.util.Objects;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UIScrollView;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;
import br.com.wdc.shopping.view.robovm.util.UIKitDom;

public class ProductViewRoboVM extends AbstractViewRoboVM<ProductPresenter> {

    private final ProductViewState state;

    private boolean notRendered = true;
    private UILabel nameLabel;
    private String nameOldValue;
    private UILabel descriptionLabel;
    private String descriptionOldValue;
    private UILabel priceLabel;
    private double priceOldValue;
    private UILabel errorLabel;

    public ProductViewRoboVM(ShoppingRoboVMApplication app, ProductPresenter presenter) {
        super("product", app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.nameLabel = null;
        this.nameOldValue = null;
        this.descriptionLabel = null;
        this.descriptionOldValue = null;
        this.priceLabel = null;
        this.priceOldValue = 0;
        this.errorLabel = null;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            var container = new UIScrollView(new CGRect(0, 0, 375, 600));
            this.rootView = container;
            UIKitDom.render(container, this::initialRender);
            this.notRendered = false;
        }

        // Sync error
        if (state.errorCode != 0) {
            errorLabel.setText(state.errorMessage);
            errorLabel.setHidden(false);
            state.errorCode = 0;
            state.errorMessage = null;
        }

        var product = state.product;
        if (product != null) {
            if (!Objects.equals(this.nameOldValue, product.name)) {
                nameLabel.setText(product.name);
                this.nameOldValue = product.name;
            }

            if (!Objects.equals(this.descriptionOldValue, product.description)) {
                descriptionLabel.setText(stripHtml(product.description));
                this.descriptionOldValue = product.description;
            }

            if (this.priceOldValue != product.price) {
                priceLabel.setText(String.format("R$ %.2f", product.price));
                this.priceOldValue = product.price;
            }
        }
    }

    @SuppressWarnings("unused")
	private void initialRender(UIKitDom dom, UIScrollView root) {
        root.setBackgroundColor(UIColor.clear());

        // Back link
        dom.button(160, 44, backBtn -> {
            backBtn.setFrame(new CGRect(4, 4, 160, 44));
            backBtn.setTitle("\u2039 Produtos", UIControlState.Normal);
            backBtn.setTitleColor(UIColor.white(), UIControlState.Normal);
            backBtn.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            backBtn.setContentHorizontalAlignment(org.robovm.apple.uikit.UIControlContentHorizontalAlignment.Left);
            backBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("back", presenter::onOpenProducts));
        });

        // Error
        dom.label(343, 20, error -> {
            this.errorLabel = error;
            error.setFrame(new CGRect(16, 52, 343, 20));
            error.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
            error.setFont(UIFont.getSystemFont(14));
            error.setHidden(true);
        });

        // Product info card
        dom.absolute(343, 230, card -> {
            card.setFrame(new CGRect(16, 80, 343, 230));
            card.setBackgroundColor(UIColor.white());
            card.getLayer().setCornerRadius(10);

            dom.label(311, 28, name -> {
                this.nameLabel = name;
                name.setFrame(new CGRect(16, 16, 311, 28));
                name.setFont(UIFont.getBoldSystemFont(22));
                name.setNumberOfLines(0);
            });

            dom.label(311, 120, desc -> {
                this.descriptionLabel = desc;
                desc.setFrame(new CGRect(16, 52, 311, 120));
                desc.setFont(UIFont.getSystemFont(15));
                desc.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
                desc.setNumberOfLines(0);
            });

            dom.label(311, 35, price -> {
                this.priceLabel = price;
                price.setFrame(new CGRect(16, 180, 311, 35));
                price.setFont(UIFont.getBoldSystemFont(28));
                price.setTextColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0));
            });
        });

        // Add to cart button
        dom.button(343, 50, addBtn -> {
            addBtn.setFrame(new CGRect(16, 326, 343, 50));
            addBtn.setTitle("Adicionar ao Carrinho", UIControlState.Normal);
            addBtn.setTitleColor(UIColor.white(), UIControlState.Normal);
            addBtn.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
            addBtn.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
            addBtn.getLayer().setCornerRadius(10);
            addBtn.addOnTouchUpInsideListener((c, e) ->
                    safeAction("addToCart", () -> presenter.onAddToCart(1)));
        });

        root.setContentSize(new CGSize(375, 400));
    }

    private static String stripHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        String text = html;
        text = text.replaceAll("(?i)<li[^>]*>", "\n\u2022 ");
        text = text.replaceAll("(?i)<br\\s*/?>|</p>|<p[^>]*>|</li>|</ul>|</ol>", "\n");
        text = text.replaceAll("<[^>]+>", "");
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&nbsp;", " ");
        text = text.replaceAll("\\n{3,}", "\n\n").trim();
        return text;
    }
}
