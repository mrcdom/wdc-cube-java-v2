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

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.robovm.AbstractViewRoboVM;
import br.com.wdc.shopping.view.robovm.ShoppingRoboVMApplication;

public class ProductViewRoboVM extends AbstractViewRoboVM<ProductPresenter> {

    private final ProductViewState state;
    private boolean built;

    private UILabel nameLabel;
    private UILabel descriptionLabel;
    private UILabel priceLabel;
    private UILabel errorLabel;

    public ProductViewRoboVM(ShoppingRoboVMApplication app, ProductPresenter presenter) {
        super("product", app, presenter);
        this.state = presenter.state;
    }

    private void buildUI() {
        var container = new UIScrollView(new CGRect(0, 0, 375, 600));
        container.setBackgroundColor(UIColor.clear());

        // Back link - white for doodle background
        var backButton = new UIButton(UIButtonType.System);
        backButton.setFrame(new CGRect(4, 4, 160, 44));
        backButton.setTitle("\u2039 Produtos", UIControlState.Normal);
        backButton.setTitleColor(UIColor.white(), UIControlState.Normal);
        backButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
        backButton.setContentHorizontalAlignment(org.robovm.apple.uikit.UIControlContentHorizontalAlignment.Left);
        backButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("back", () -> presenter.onOpenProducts()));
        container.addSubview(backButton);

        // Error
        errorLabel = new UILabel(new CGRect(16, 52, 343, 20));
        errorLabel.setTextColor(UIColor.fromRGBA(1.0, 0.23, 0.19, 1.0));
        errorLabel.setFont(UIFont.getSystemFont(14));
        errorLabel.setHidden(true);
        container.addSubview(errorLabel);

        // Product info card
        var card = new UIView(new CGRect(16, 80, 343, 230));
        card.setBackgroundColor(UIColor.white());
        card.getLayer().setCornerRadius(10);
        container.addSubview(card);

        // Product name
        nameLabel = new UILabel(new CGRect(16, 16, 311, 28));
        nameLabel.setFont(UIFont.getBoldSystemFont(22));
        nameLabel.setNumberOfLines(0);
        card.addSubview(nameLabel);

        // Description
        descriptionLabel = new UILabel(new CGRect(16, 52, 311, 120));
        descriptionLabel.setFont(UIFont.getSystemFont(15));
        descriptionLabel.setTextColor(UIColor.fromRGBA(0.56, 0.56, 0.58, 1.0));
        descriptionLabel.setNumberOfLines(0);
        card.addSubview(descriptionLabel);

        // Price
        priceLabel = new UILabel(new CGRect(16, 180, 311, 35));
        priceLabel.setFont(UIFont.getBoldSystemFont(28));
        priceLabel.setTextColor(UIColor.fromRGBA(0.0, 0.48, 1.0, 1.0));
        card.addSubview(priceLabel);

        // Add to cart button - full width, iOS tinted style
        var addToCartButton = new UIButton(UIButtonType.System);
        addToCartButton.setFrame(new CGRect(16, 326, 343, 50));
        addToCartButton.setTitle("Adicionar ao Carrinho", UIControlState.Normal);
        addToCartButton.setTitleColor(UIColor.white(), UIControlState.Normal);
        addToCartButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(17));
        addToCartButton.setBackgroundColor(UIColor.fromRGBA(0.20, 0.78, 0.35, 1.0));
        addToCartButton.getLayer().setCornerRadius(10);
        addToCartButton.addOnTouchUpInsideListener((c, e) ->
                safeAction("addToCart", () -> presenter.onAddToCart(1)));
        container.addSubview(addToCartButton);

        container.setContentSize(new org.robovm.apple.coregraphics.CGSize(375, 400));
        this.rootView = container;
        this.built = true;
    }

    @Override
    public void doUpdate() {
        if (!built) {
            buildUI();
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
            nameLabel.setText(product.name);
            descriptionLabel.setText(stripHtml(product.description));
            priceLabel.setText(String.format("R$ %.2f", product.price));
        }
    }

    /**
     * Simple HTML-to-text converter: replaces list items with bullet points,
     * strips remaining tags, and collapses whitespace.
     */
    private static String stripHtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        String text = html;
        // Convert <li> to bullet points
        text = text.replaceAll("(?i)<li[^>]*>", "\n\u2022 ");
        // Convert <br>, <p>, </p>, </li>, </ul>, </ol> to newlines
        text = text.replaceAll("(?i)<br\\s*/?>|</p>|<p[^>]*>|</li>|</ul>|</ol>", "\n");
        // Remove all remaining HTML tags
        text = text.replaceAll("<[^>]+>", "");
        // Decode common HTML entities
        text = text.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&nbsp;", " ");
        // Collapse multiple blank lines and trim
        text = text.replaceAll("\\n{3,}", "\n\n").trim();
        return text;
    }
}
