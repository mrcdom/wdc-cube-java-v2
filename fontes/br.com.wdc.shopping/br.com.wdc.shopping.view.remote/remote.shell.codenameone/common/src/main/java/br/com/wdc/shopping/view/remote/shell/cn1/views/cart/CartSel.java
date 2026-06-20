package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import br.com.wdc.shopping.view.remote.shell.cn1.Sel;

/** Seletores (UIIDs) do carrinho — espelha {@code views/cart/_cart.scss}. Herda os globais. */
public final class CartSel extends Sel {

    public static final CartSel INSTANCE = new CartSel();

    private CartSel() {
        // singleton
    }

    public final String CART_PAGE = "CrtCartPage";
    public final String CART_CARD = "CrtCartCard";
    public final String CART_ITEM_ROW = "CrtCartItemRow";
    public final String CART_ITEM_NAME = "CrtCartItemName";
    public final String CART_STEPPER = "CrtCartStepper";
    public final String CART_ITEM_SUBTOTAL = "CrtCartItemSubtotal";
    public final String CART_REMOVE_BTN = "CrtCartRemoveBtn";
    public final String CART_FOOTER = "CrtCartFooter";
    public final String CART_FOOTER_LABEL = "CrtCartFooterLabel";
    public final String CART_FOOTER_TOTAL = "CrtCartFooterTotal";
    public final String CART_ACTIONS = "CrtCartActions";
    public final String CART_EMPTY = "CrtCartEmpty";
    public final String CART_EMPTY_ICON_BOX = "CrtCartEmptyIconBox";
    public final String CART_EMPTY_TITLE = "CrtCartEmptyTitle";
    public final String CART_EMPTY_SUB = "CrtCartEmptySub";
}
