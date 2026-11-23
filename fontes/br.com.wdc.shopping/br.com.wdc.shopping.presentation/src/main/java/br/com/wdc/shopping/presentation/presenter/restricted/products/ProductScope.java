package br.com.wdc.shopping.presentation.presenter.restricted.products;

import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

/** What the product page draws ({@link ProductPresenter}): the product and the add-to-cart action. */
public class ProductScope extends Scope {

    public static final String CLASS_ID = "48b693f67410";

    public final Prop<ProductInfo> product = prop();
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    public ActionOf<Integer> onAddToCart = ActionOf.noop();
    public Action onOpenProducts = Action.NOOP;

}
