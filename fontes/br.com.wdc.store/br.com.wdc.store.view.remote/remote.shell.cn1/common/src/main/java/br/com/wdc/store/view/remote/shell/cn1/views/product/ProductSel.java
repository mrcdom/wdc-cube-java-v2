package br.com.wdc.store.view.remote.shell.cn1.views.product;

import br.com.wdc.store.view.remote.shell.cn1.Sel;

/** Product detail selectors (UIIDs) — mirrors {@code views/product/_product.scss}. Inherits the global ones.
 */
public final class ProductSel extends Sel {

    public static final ProductSel INSTANCE = new ProductSel();

    private ProductSel() {
        // singleton
    }

    public final String PRODUCT_PAGE = "PrdPage";
    public final String PRODUCT_TITLE = "PrdTitle";
    public final String PRODUCT_DIVIDER = "PrdDivider";
    public final String PRODUCT_DESC_CARD = "PrdDescCard";
    public final String PRODUCT_DESC_TEXT = "PrdDescText";
    public final String PRODUCT_PRICE_IMAGE_ROW = "PrdPriceImageRow";
    public final String PRODUCT_PRICE_BADGE = "PrdPriceBadge";
    public final String QTY_LABEL = "PrdQtyLabel";
    public final String PRODUCT_IMAGE_BOX = "PrdImageBox";
}
