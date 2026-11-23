package br.com.wdc.shopping.presentation.presenter.restricted.home;

import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

/**
 * What the home draws ({@link HomePresenter}): the header, its two panels and the scope of the presenter below. The
 * panels are segments: scopes of their own, with their own views, that exist only inside this one.
 */
public class HomeScope extends Scope {

    public static final String CLASS_ID = "473dbdd7a36a";

    /**
     * The products panel: a segment of the home, with no presenter of its own. The home presenter binds its
     * action; the view layer draws it with the view registered for this class.
     */
    public static class ProductsPanelScope extends Scope {

        public static final String CLASS_ID = "a1b2c3d4e5f6";

        public final Prop<List<ProductInfo>> products = prop();

        public ActionOf<Long> onOpenProduct = ActionOf.noop();

    }

    /** The purchases panel: a segment of the home, like {@link ProductsPanelScope}. */
    public static class PurchasesPanelScope extends Scope {

        public static final String CLASS_ID = "b3c4d5e6f7a8";

        public final Prop<List<PurchaseInfo>> purchases = prop(Collections.emptyList());
        public final Prop<Integer> page = prop(0);
        public final Prop<Integer> pageSize = prop(-1);
        public final Prop<Integer> totalCount = prop(0);

        public ActionOf<Integer> onPageChange = ActionOf.noop();
        public ActionOf<Integer> onItemSizeCapacityChanged = ActionOf.noop();
        public ActionOf<Long> onOpenReceipt = ActionOf.noop();

    }

    /** The scope of the presenter below (product, cart, receipt), or none for the home's own panels. */
    public final Prop<Scope> content = prop();
    public final Prop<ProductsPanelScope> productsPanel = prop();
    public final Prop<PurchasesPanelScope> purchasesPanel = prop();
    public final Prop<String> nickName = prop();
    public final Prop<Integer> cartItemCount = prop(0);
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    public Action onOpenCart = Action.NOOP;
    public Action onExit = Action.NOOP;

}
