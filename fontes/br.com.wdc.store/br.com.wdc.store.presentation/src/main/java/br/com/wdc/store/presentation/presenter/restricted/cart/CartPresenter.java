package br.com.wdc.store.presentation.presenter.restricted.cart;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.store.domain.exception.InvalidCartItemException;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptKeys;

public class CartPresenter extends AbstractCubePresenter<StoreApplication> {

    // :: Private Class Fields

    private static final Log LOG = Log.getLogger(CartPresenter.class);

    // :: Public Instance Fields

    public final CartScope scope = bind(new CartScope());

    // :: Internal Instance Fields

    private CartManager cart;
    private ScopeSlot ownerSlot;
    private Registration pendingErrorClear;

    /** The rows by product id: a row is reused while its item stays in the cart. */
    private final Map<Long, CartScope.CartItemScope> rowsById = new LinkedHashMap<>();

    // :: Constructor

    public CartPresenter(StoreApplication app) {
        super(app);
        this.cart = app.getCart();
        this.scope.onBuy = action(1, this::buy);
        // Codes 2 and 4 were the rows' before they had views of their own: now each row has its actions
        this.scope.onOpenProducts = action(3, () -> new HomeKeys(this.app).flip());
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        this.syncRows();

        // Read on every call: a reused presenter goes where its parent puts it now, not where it was first put
        this.ownerSlot = new CartKeys(this.app, intent).parentSlot();
        if (initialization) {
        }

        this.ownerSlot.setScope(this.scope);

        return true;
    }

    // :: User Actions

    private void modifyQuantity(long productId, Integer quantity) {
        if (quantity == null) {
            this.errorMalformedQuantityValue();
            LOG.warn("modifyQuantity.errorMalformedQuantityValue: {}", this.scope.errorMessage.get());
            return;
        }

        if (quantity < 1) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.warn("modifyQuantity.alertThereIsItemWhichValueIsLessThanOne: {}", this.scope.errorMessage.get());
            return;
        }

        var found = this.cart.modifyProductQuantity(productId, quantity);
        if (!found) {
            this.alertProductNotFound();
            LOG.warn("modifyQuantity.alertProductNotFound: {}", this.scope.errorMessage.get());
        } else {
            this.scope.errorCode.set(0);
            this.scope.errorMessage.set(null);
            this.syncRows();
        }
    }

    private void removeProduct(long productId) {
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
     * Brings the rows in line with the cart: a new item gets a row (the view layer draws it), a row whose item stays
     * is reused and only its changed props redraw it, a row whose item left is detached (its view released).
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

        this.scope.rows.set(next);
        this.scope.total.set(total);
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
            LOG.warn("buy: {}", this.scope.errorMessage.get());
            return;
        }

        try {
            var purchaseId = this.cart.commit(this.app.getSubject());

            new ReceiptKeys(this.app)
                    .withPurchaseId(purchaseId)
                    .withPurchaseMade(true)
                    .flip();
        } catch (InvalidCartItemException caught) {
            this.alertThereIsItemWhichValueIsLessThanOne();
            LOG.error("buy.alertThereIsItemWhichValueIsLessThanOne: {}", this.scope.errorMessage.get(), caught);
        } catch (OfflineException caught) {
            this.alertDatabaseOffline();
            LOG.error("buy.alertDatabaseOffline: {}", this.scope.errorMessage.get(), caught);
        }
    }

    // :: Message Methods

    private void clearErrorAfterDelay() {
        if (this.pendingErrorClear != null) {
            this.pendingErrorClear.remove();
        }
        this.pendingErrorClear = ScheduledExecutor.BEAN.get().schedule(() -> {
            this.scope.errorCode.set(0);
            this.scope.errorMessage.set(null);
        }, Duration.ofSeconds(3));
    }

    protected void alertThereIsItemWhichValueIsLessThanOne() {
        this.scope.errorCode.set(1);
        this.scope.errorMessage.set("The cart must have at least one item to complete a purchase");
        this.clearErrorAfterDelay();
    }

    protected void alertProductNotFound() {
        this.scope.errorCode.set(2);
        this.scope.errorMessage.set("Product not found in the database.");
        this.clearErrorAfterDelay();
    }

    protected void alertPurchaseOfEmptyCart() {
        this.scope.errorCode.set(3);
        this.scope.errorMessage.set("Some products have a quantity of less than one item. Unable to purchase.");
        this.clearErrorAfterDelay();
    }

    protected void alertDatabaseOffline() {
        this.scope.errorCode.set(4);
        this.scope.errorMessage.set("The database is currently offline. Please wait a moment and try again.");
        this.clearErrorAfterDelay();
    }

    protected void errorMalformedQuantityValue() {
        this.scope.errorCode.set(6);
        this.scope.errorMessage.set("The quantity value is malformed.");
        this.clearErrorAfterDelay();
    }

}
