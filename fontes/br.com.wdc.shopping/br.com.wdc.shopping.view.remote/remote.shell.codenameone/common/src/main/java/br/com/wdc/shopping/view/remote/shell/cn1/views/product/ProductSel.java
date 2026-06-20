package br.com.wdc.shopping.view.remote.shell.cn1.views.product;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do detalhe do produto — espelha {@code views/product/_product.scss}. Herda os globais. */
public final class ProductSel extends Sel {

    public static final ProductSel INSTANCE = new ProductSel();

    private ProductSel() {
        // singleton
    }

    public final String PRODUCT_PAGE = "PrdProductPage";
    public final String PRODUCT_TITLE = "PrdProductTitle";
    public final String PRODUCT_DIVIDER = "PrdProductDivider";
    public final String PRODUCT_DESC_CARD = "PrdProductDescCard";
    public final String PRODUCT_DESC_TEXT = "PrdProductDescText";
    public final String PRODUCT_PRICE_IMAGE_ROW = "PrdProductPriceImageRow";
    public final String PRODUCT_PRICE_BADGE = "PrdProductPriceBadge";
    public final String QTY_LABEL = "PrdQtyLabel";
    public final String PRODUCT_IMAGE_BOX = "PrdProductImageBox";
}
