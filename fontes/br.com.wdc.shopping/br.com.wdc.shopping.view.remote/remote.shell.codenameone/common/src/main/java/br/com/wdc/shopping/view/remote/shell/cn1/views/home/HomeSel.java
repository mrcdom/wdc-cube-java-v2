package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/**
 * Seletores (UIIDs) do pacote home — app bar, grade de produtos, abas e histórico.
 * Espelha {@code views/home/_home.scss}. Herda os globais.
 */
public final class HomeSel extends Sel {

    public static final HomeSel INSTANCE = new HomeSel();

    private HomeSel() {
        // singleton
    }

    // Grade de produtos
    public final String PRODUCTS_PANEL = "ProductsPanel";
    public final String PRODUCT_CARD = "ProductCard";
    public final String PRODUCT_CARD_IMAGE = "ProductCardImage";
    public final String PRODUCT_CARD_NAME = "ProductCardName";
    public final String PRODUCT_CARD_PRICE = "ProductCardPrice";

    // App bar
    public final String APP_BAR = "AppBar";
    public final String APP_BAR_BTN = "AppBarBtn";
    public final String GREETING_SMALL = "GreetingSmall";
    public final String GREETING_NAME = "GreetingName";
    public final String APP_BAR_LOGO_BOX = "AppBarLogoBox";
    public final String APP_BAR_BRAND = "AppBarBrand";
    public final String APP_BAR_BRAND_SUB = "AppBarBrandSub";
    public final String CART_BADGE = "CartBadge";

    // Abas (compacto)
    public final String TAB_NAV = "TabNav";
    public final String TAB_ITEM = "TabItem";
    public final String TAB_ITEM_ACTIVE = "TabItemActive";

    // Histórico
    public final String PURCHASES_PANEL = "PurchasesPanel";
    public final String PURCHASES_HEADER_ICON = "PurchasesHeaderIcon";
    public final String PURCHASES_TITLE = "PurchasesTitle";
    public final String PURCHASES_HINT = "PurchasesHint";
    public final String PURCHASE_CARD = "PurchaseCard";
    public final String PURCHASE_LINE = "PurchaseLine";
    public final String PURCHASE_ID = "PurchaseId";
    public final String PURCHASE_DATE = "PurchaseDate";
    public final String PURCHASE_ITEMS = "PurchaseItems";
    public final String PURCHASE_TOTAL = "PurchaseTotal";
    public final String PURCHASE_PAGINATION = "PurchasePagination";
    public final String PURCHASE_PAGE_PILL = "PurchasePagePill";
    public final String PURCHASE_PAGE_BTN = "PurchasePageBtn";
    public final String PURCHASE_PAGE_INFO = "PurchasePageInfo";
}
