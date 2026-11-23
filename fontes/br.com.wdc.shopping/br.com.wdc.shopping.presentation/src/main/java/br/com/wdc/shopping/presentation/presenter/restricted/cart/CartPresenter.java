package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.ActionOf2;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.domain.exception.InvalidCartItemException;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

public class CartPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(CartPresenter.class);

    // :: Public class Fields

    public static Function<CartPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class CartScope extends Scope {

        /**
         * A row of the cart: a segment with its own view and its own actions, bound to its item — the row does not
         * send the product id back, it is the item.
         */
        public static class CartItemScope extends Scope {

            public static final String CLASS_ID = "c8a1f0e2b7d4";

            public static BiFunction<ShoppingApplication, CartItemScope, CubeView> createView;

            public final Prop<Long> id = prop();
            public final Prop<String> name = prop();
            public final Prop<String> image = prop();
            public final Prop<Double> price = prop(0.0);
            public final Prop<Integer> quantity = prop(0);

            public Action onRemove = Action.NOOP;
            public ActionOf<Integer> onModifyQuantity = ActionOf.noop();

        }

        /** The rows, in the cart's order; changing a quantity redraws only its row (and the total). */
        public final Prop<List<CartItemScope>> rows = prop(Collections.emptyList());
        public final Prop<Double> total = prop(0.0);

        /**
         * The items inline, as before the rows: kept while the remote clients migrate to {@link #rows}.
         * @deprecated goes away with the compatibility (codes 2 and 4 of the cart, too)
         */
        @Deprecated
        public final Prop<List<CartItem>> items = prop();
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();

        public ActionOf2<Long, Integer> onModifyQuantity = ActionOf2.noop();
        public ActionOf<Long> onRemoveProduct = ActionOf.noop();
        public Action onBuy = Action.NOOP;
        public Action onOpenProducts = Action.NOOP;

    }

    public final CartScope state = bind(new CartScope());

    // :: Internal Instance Fields

    private CartManager cart;
    private CubeViewSlot ownerSlot;
    private Registration pendingErrorClear;

    /** The rows by product id: a row is reused while its item stays in the cart. */
    private final Map<Long, CartScope.CartItemScope> rowsById = new LinkedHashMap<>();

    // :: Constructor

    public CartPresenter(ShoppingApplication app) {
        super(app);
        this.cart = app.getCart();
        this.state.items.set(Collections.emptyList());
        this.state.onBuy = action(1, this::buy);
        // Codes 2 and 4: the rows before they had views of their own (compatibility, while the clients migrate)
        this.state.onRemoveProduct = actionOf(2, FormParam.asLong("p.productId"), this::removeProduct);
        this.state.onOpenProducts = action(3, () -> new HomeKeys(this.app).flip());
        this.state.onModifyQuantity = actionOf2(4, FormParam.asLong("p.productId"), FormParam.asInteger("p.quantity"),
                this::modifyQuantity);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.syncRows();

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = new CartKeys(this.app, intent).parentSlot();
        if (initialization) {
            this.view = createView.apply(this);
        }

        this.ownerSlot.setView(this.view);

        return true;
    }

    // :: User Actions

    private void modifyQuantity(Long productId, Integer quantity) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("modifyQuantity.errorCodigoDeProdutoMalFormatado: {}", this.state.errorMessage.get());
            return;
        }

        if (quantity == null) {
            this.errorValorQuantidadeMalFormatado();
            LOG.warn("modifyQuantity.errorValorQuantidadeMalFormatado: {}", this.state.errorMessage.get());
            return;
        }

        if (quantity < 1) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.warn("modifyQuantity.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage.get());
            return;
        }

        var found = this.cart.modifyProductQuantity(productId, quantity);
        if (!found) {
            this.alertProductNotFound();
            LOG.warn("modifyQuantity.alertProductNotFound: {}", this.state.errorMessage.get());
        } else {
            this.state.errorCode.set(0);
            this.state.errorMessage.set(null);
            this.syncRows();
        }
    }

    private void removeProduct(Long productId) {
        if (productId == null) {
            this.errorCodigoDeProdutoMalFormatado();
            LOG.warn("removeProduct: {}", this.state.errorMessage.get());
            return;
        }

        var modified = this.cart.removeProduct(productId);
        if (modified) {
            if (this.cart.getSize() == 0) {
                new HomeKeys(this.app).flip();
            } else {
                this.syncRows();
            }
        }
    }

    // :: Rows

    /**
     * Brings the rows in line with the cart: a new item gets a row (and a view), a row whose item stays is reused and
     * only its changed props redraw it, a row whose item left is detached.
     */
    private void syncRows() {
        var items = this.cart.getCartItems();
        var next = new ArrayList<CartScope.CartItemScope>(items.size());
        var present = new HashSet<Long>();
        var total = 0.0;

        for (var item : items) {
            var row = this.rowsById.get(item.id);
            if (row == null) {
                row = this.newRow(item.id);
                this.rowsById.put(item.id, row);
                attach(row, CartScope.CartItemScope.createView);
            }
            row.name.set(item.name);
            row.image.set(item.image);
            row.price.set(item.price);
            row.quantity.set(item.quantity);

            present.add(item.id);
            next.add(row);
            total += item.price * item.quantity;
        }

        var it = this.rowsById.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!present.contains(entry.getKey())) {
                detach(entry.getValue());
                it.remove();
            }
        }

        this.state.rows.set(next);
        this.state.total.set(total);

        // Compatibility: the inline items change in place, so the prop alone would not redraw them
        this.state.items.set(items);
        this.update();
    }

    private CartScope.CartItemScope newRow(long productId) {
        var row = new CartScope.CartItemScope();
        row.id.set(productId);
        row.onRemove = action(row, 1, () -> this.removeProduct(productId));
        row.onModifyQuantity = actionOf(row, 2, FormParam.asInteger("p.quantity"),
                quantity -> this.modifyQuantity(productId, quantity));
        return row;
    }

    private void buy() {
        if (this.cart.getSize() == 0) {
            this.alertPurchaseOfEmptyCart();
            LOG.warn("buy: {}", this.state.errorMessage.get());
            return;
        }

        try {
            var purchaseId = this.cart.commit(this.app.getSubject());

            new ReceiptKeys(this.app).withPurchaseId(purchaseId).withPurchaseMade(true).flip();
        } catch (InvalidCartItemException caught) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.error("buy.alertThereIsItemWhichValueIsLessThanOne: {}", this.state.errorMessage.get(), caught);
        } catch (OfflineException caught) {
            this.alertDatabaseOffline();
            LOG.error("buy.alertDatabaseOffline: {}", this.state.errorMessage.get(), caught);
        }
    }

    // :: Message Methods

    private void clearErrorAfterDelay() {
        if (this.pendingErrorClear != null) {
            this.pendingErrorClear.remove();
        }
        this.pendingErrorClear = ScheduledExecutor.BEAN.get().schedule(() -> {
            this.state.errorCode.set(0);
            this.state.errorMessage.set(null);
        }, Duration.ofSeconds(3));
    }

    protected void alertThereIsItemWhichValueIsLessThanOne() {
        this.state.errorCode.set(1);
        this.state.errorMessage.set("Deve existir pelo menos um item no carrinhro para se efetivar uma compra");
        this.clearErrorAfterDelay();
    }

    protected void alertProductNotFound() {
        this.state.errorCode.set(2);
        this.state.errorMessage.set("Produdo não localizado na base dados.");
        this.clearErrorAfterDelay();
    }

    protected void alertPurchaseOfEmptyCart() {
        this.state.errorCode.set(3);
        this.state.errorMessage.set("Existem produtos com menos de um item na quantidade. Impossível comprar.");
        this.clearErrorAfterDelay();
    }

    protected void alertDatabaseOffline() {
        this.state.errorCode.set(4);
        this.state.errorMessage.set("O banco de dados encontra-se fora do ar no momento. Aguarde alguns instantes e tente novamente.");
        this.clearErrorAfterDelay();
    }

    protected void errorCodigoDeProdutoMalFormatado() {
        this.state.errorCode.set(5);
        this.state.errorMessage.set("Código do produto mal formado.");
        this.clearErrorAfterDelay();
    }

    protected void errorValorQuantidadeMalFormatado() {
        this.state.errorCode.set(6);
        this.state.errorMessage.set("Valor da quantiade está mal formado.");
        this.clearErrorAfterDelay();
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("7eb485e5f843");
    }

}
