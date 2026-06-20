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
    public final String PRODUCTS_PANEL = "ProductsPanelHom";
    public final String PRODUCT_CARD = "ProductCardHom";
    public final String PRODUCT_CARD_IMAGE = "ProductCardImageHom";
    public final String PRODUCT_CARD_NAME = "ProductCardNameHom";
    public final String PRODUCT_CARD_PRICE = "ProductCardPriceHom";

    // App bar
    public final String APP_BAR = "AppBarHom";
    public final String APP_BAR_BTN = "AppBarBtnHom";
    public final String GREETING_SMALL = "GreetingSmallHom";
    public final String GREETING_NAME = "GreetingNameHom";
    public final String APP_BAR_LOGO_BOX = "AppBarLogoBoxHom";
    public final String APP_BAR_BRAND = "AppBarBrandHom";
    public final String APP_BAR_BRAND_SUB = "AppBarBrandSubHom";
    public final String CART_BADGE = "CartBadgeHom";

    // Abas (compacto)
    public final String TAB_NAV = "TabNavHom";
    public final String TAB_ITEM = "TabItemHom";
    public final String TAB_ITEM_ACTIVE = "TabItemActiveHom";

    // Histórico
    public final String PURCHASES_PANEL = "PurchasesPanelHom";
    public final String PURCHASES_HEADER_ICON = "PurchasesHeaderIconHom";
    public final String PURCHASES_TITLE = "PurchasesTitleHom";
    public final String PURCHASES_HINT = "PurchasesHintHom";
    public final String PURCHASE_CARD = "PurchaseCardHom";
    public final String PURCHASE_LINE = "PurchaseLineHom";
    public final String PURCHASE_ID = "PurchaseIdHom";
    public final String PURCHASE_DATE = "PurchaseDateHom";
    public final String PURCHASE_ITEMS = "PurchaseItemsHom";
    public final String PURCHASE_TOTAL = "PurchaseTotalHom";
    public final String PURCHASE_PAGINATION = "PurchasePaginationHom";
    public final String PURCHASE_PAGE_PILL = "PurchasePagePillHom";
    public final String PURCHASE_PAGE_BTN = "PurchasePageBtnHom";
    public final String PURCHASE_PAGE_INFO = "PurchasePageInfoHom";
}
