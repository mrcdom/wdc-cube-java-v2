package br.com.wdc.shopping.view.remote.shell.cn1.views.product;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do detalhe do produto — espelha {@code views/product/_product.scss}. Herda os globais. */
public final class ProductSel extends Sel {

    public static final ProductSel INSTANCE = new ProductSel();

    private ProductSel() {
        // singleton
    }

    public final String PRODUCT_PAGE = "ProductPagePrd";
    public final String PRODUCT_TITLE = "ProductTitlePrd";
    public final String PRODUCT_DIVIDER = "ProductDividerPrd";
    public final String PRODUCT_DESC_CARD = "ProductDescCardPrd";
    public final String PRODUCT_DESC_TEXT = "ProductDescTextPrd";
    public final String PRODUCT_PRICE_IMAGE_ROW = "ProductPriceImageRowPrd";
    public final String PRODUCT_PRICE_BADGE = "ProductPriceBadgePrd";
    public final String QTY_LABEL = "QtyLabelPrd";
    public final String PRODUCT_IMAGE_BOX = "ProductImageBoxPrd";
}
