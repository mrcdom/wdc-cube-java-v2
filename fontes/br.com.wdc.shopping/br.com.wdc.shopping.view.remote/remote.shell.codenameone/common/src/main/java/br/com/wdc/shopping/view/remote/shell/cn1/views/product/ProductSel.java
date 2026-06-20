package br.com.wdc.shopping.view.remote.shell.cn1.views.product;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do detalhe do produto — espelha {@code views/product/_product.scss}. Herda os globais. */
public final class ProductSel extends Sel {

    public static final ProductSel INSTANCE = new ProductSel();

    private ProductSel() {
        // singleton
    }

    public final String PRODUCT_PAGE = "ProductPage";
    public final String PRODUCT_TITLE = "ProductTitle";
    public final String PRODUCT_DIVIDER = "ProductDivider";
    public final String PRODUCT_DESC_CARD = "ProductDescCard";
    public final String PRODUCT_DESC_TEXT = "ProductDescText";
    public final String PRODUCT_PRICE_IMAGE_ROW = "ProductPriceImageRow";
    public final String PRODUCT_PRICE_BADGE = "ProductPriceBadge";
    public final String QTY_LABEL = "QtyLabel";
    public final String PRODUCT_IMAGE_BOX = "ProductImageBox";
}
