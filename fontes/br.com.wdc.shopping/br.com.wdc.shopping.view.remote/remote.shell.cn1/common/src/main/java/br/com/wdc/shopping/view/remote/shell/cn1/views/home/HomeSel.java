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
    public final String PRODUCTS_PANEL = "HomProductsPanel";
    public final String PRODUCT_CARD = "HomProductCard";
    public final String PRODUCT_CARD_IMAGE = "HomProductCardImage";
    public final String PRODUCT_CARD_NAME = "HomProductCardName";
    public final String PRODUCT_CARD_PRICE = "HomProductCardPrice";

    // App bar
    public final String APP_BAR = "HomAppBar";
    public final String APP_BAR_BTN = "HomAppBarBtn";
    public final String GREETING_SMALL = "HomGreetingSmall";
    public final String GREETING_NAME = "HomGreetingName";
    public final String APP_BAR_LOGO_BOX = "HomAppBarLogoBox";
    public final String APP_BAR_BRAND = "HomAppBarBrand";
    public final String APP_BAR_BRAND_SUB = "HomAppBarBrandSub";
    public final String CART_BADGE = "HomCartBadge";

    // Abas (compacto)
    public final String TAB_NAV = "HomTabNav";
    public final String TAB_ITEM = "HomTabItem";
    public final String TAB_ITEM_ACTIVE = "HomTabItemActive";

    // Histórico
    public final String PURCHASES_PANEL = "HomPurchasesPanel";
    public final String PURCHASES_HEADER_ICON = "HomPurchasesHeaderIcon";
    public final String PURCHASES_TITLE = "HomPurchasesTitle";
    public final String PURCHASES_HINT = "HomPurchasesHint";
    public final String PURCHASE_CARD = "HomPurchaseCard";
    public final String PURCHASE_LINE = "HomPurchaseLine";
    public final String PURCHASE_ID = "HomPurchaseId";
    public final String PURCHASE_DATE = "HomPurchaseDate";
    public final String PURCHASE_ITEMS = "HomPurchaseItems";
    public final String PURCHASE_TOTAL = "HomPurchaseTotal";
    public final String PURCHASE_PAGINATION = "HomPurchasePagination";
    public final String PURCHASE_PAGE_PILL = "HomPurchasePagePill";
    public final String PURCHASE_PAGE_BTN = "HomPurchasePageBtn";
    public final String PURCHASE_PAGE_INFO = "HomPurchasePageInfo";
}
