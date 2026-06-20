package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do carrinho — espelha {@code views/cart/_cart.scss}. Herda os globais. */
public final class CartSel extends Sel {

    public static final CartSel INSTANCE = new CartSel();

    private CartSel() {
        // singleton
    }

    public final String CART_PAGE = "CartPageCrt";
    public final String CART_CARD = "CartCardCrt";
    public final String CART_ITEM_ROW = "CartItemRowCrt";
    public final String CART_ITEM_NAME = "CartItemNameCrt";
    public final String CART_STEPPER = "CartStepperCrt";
    public final String CART_ITEM_SUBTOTAL = "CartItemSubtotalCrt";
    public final String CART_REMOVE_BTN = "CartRemoveBtnCrt";
    public final String CART_FOOTER = "CartFooterCrt";
    public final String CART_FOOTER_LABEL = "CartFooterLabelCrt";
    public final String CART_FOOTER_TOTAL = "CartFooterTotalCrt";
    public final String CART_ACTIONS = "CartActionsCrt";
    public final String CART_EMPTY = "CartEmptyCrt";
    public final String CART_EMPTY_ICON_BOX = "CartEmptyIconBoxCrt";
    public final String CART_EMPTY_TITLE = "CartEmptyTitleCrt";
    public final String CART_EMPTY_SUB = "CartEmptySubCrt";
}
