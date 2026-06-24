package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do carrinho — espelha {@code views/cart/_cart.scss}. Herda os globais. */
public final class CartSel extends Sel {

    public static final CartSel INSTANCE = new CartSel();

    private CartSel() {
        // singleton
    }

    public final String CART_PAGE = "CrtPage";
    public final String CART_CARD = "CrtCard";
    public final String CART_ITEM_ROW = "CrtItemRow";
    public final String CART_ITEM_NAME = "CrtItemName";
    public final String CART_ITEM_SUBTOTAL = "CrtItemSubtotal";
    public final String CART_REMOVE_BTN = "CrtRemoveBtn";
    public final String CART_FOOTER = "CrtFooter";
    public final String CART_FOOTER_LABEL = "CrtFooterLabel";
    public final String CART_FOOTER_TOTAL = "CrtFooterTotal";
    public final String CART_ACTIONS = "CrtActions";
    public final String CART_EMPTY = "CrtEmpty";
    public final String CART_EMPTY_ICON_BOX = "CrtEmptyIconBox";
    public final String CART_EMPTY_TITLE = "CrtEmptyTitle";
    public final String CART_EMPTY_SUB = "CrtEmptySub";
}
