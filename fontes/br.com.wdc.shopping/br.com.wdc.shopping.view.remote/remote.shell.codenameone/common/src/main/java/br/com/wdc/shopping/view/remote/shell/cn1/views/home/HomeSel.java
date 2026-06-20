package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

/**
 * Seletores (UIIDs) do pacote home — app bar, grade de produtos, abas e histórico.
 * Espelha {@code views/home/_home.scss}.
 */
public final class HomeSel {

    private HomeSel() {
        // NOOP
    }

    // Grade de produtos
    public static final String PRODUCTS_PANEL = "ProductsPanel";
    public static final String PRODUCT_CARD = "ProductCard";
    public static final String PRODUCT_CARD_IMAGE = "ProductCardImage";
    public static final String PRODUCT_CARD_NAME = "ProductCardName";
    public static final String PRODUCT_CARD_PRICE = "ProductCardPrice";

    // App bar
    public static final String APP_BAR = "AppBar";
    public static final String APP_BAR_BTN = "AppBarBtn";
    public static final String GREETING_SMALL = "GreetingSmall";
    public static final String GREETING_NAME = "GreetingName";
    public static final String APP_BAR_LOGO_BOX = "AppBarLogoBox";
    public static final String APP_BAR_BRAND = "AppBarBrand";
    public static final String APP_BAR_BRAND_SUB = "AppBarBrandSub";
    public static final String CART_BADGE = "CartBadge";

    // Abas (compacto)
    public static final String TAB_NAV = "TabNav";
    public static final String TAB_ITEM = "TabItem";
    public static final String TAB_ITEM_ACTIVE = "TabItemActive";

    // Histórico
    public static final String PURCHASES_PANEL = "PurchasesPanel";
    public static final String PURCHASES_HEADER_ICON = "PurchasesHeaderIcon";
    public static final String PURCHASES_TITLE = "PurchasesTitle";
    public static final String PURCHASES_HINT = "PurchasesHint";
    public static final String PURCHASE_CARD = "PurchaseCard";
    public static final String PURCHASE_LINE = "PurchaseLine";
    public static final String PURCHASE_ID = "PurchaseId";
    public static final String PURCHASE_DATE = "PurchaseDate";
    public static final String PURCHASE_ITEMS = "PurchaseItems";
    public static final String PURCHASE_TOTAL = "PurchaseTotal";
    public static final String PURCHASE_PAGINATION = "PurchasePagination";
    public static final String PURCHASE_PAGE_PILL = "PurchasePagePill";
    public static final String PURCHASE_PAGE_BTN = "PurchasePageBtn";
    public static final String PURCHASE_PAGE_INFO = "PurchasePageInfo";
}
