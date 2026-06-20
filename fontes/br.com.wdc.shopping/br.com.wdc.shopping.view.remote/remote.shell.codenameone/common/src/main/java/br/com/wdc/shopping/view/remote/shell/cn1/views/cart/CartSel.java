package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do carrinho — espelha {@code views/cart/_cart.scss}. Herda os globais. */
public final class CartSel extends Sel {

    public static final CartSel INSTANCE = new CartSel();

    private CartSel() {
        // singleton
    }

    public final String CART_PAGE = "CartPage";
    public final String CART_CARD = "CartCard";
    public final String CART_ITEM_ROW = "CartItemRow";
    public final String CART_ITEM_NAME = "CartItemName";
    public final String CART_STEPPER = "CartStepper";
    public final String CART_ITEM_SUBTOTAL = "CartItemSubtotal";
    public final String CART_REMOVE_BTN = "CartRemoveBtn";
    public final String CART_FOOTER = "CartFooter";
    public final String CART_FOOTER_LABEL = "CartFooterLabel";
    public final String CART_FOOTER_TOTAL = "CartFooterTotal";
    public final String CART_ACTIONS = "CartActions";
    public final String CART_EMPTY = "CartEmpty";
    public final String CART_EMPTY_ICON_BOX = "CartEmptyIconBox";
    public final String CART_EMPTY_TITLE = "CartEmptyTitle";
    public final String CART_EMPTY_SUB = "CartEmptySub";
}
