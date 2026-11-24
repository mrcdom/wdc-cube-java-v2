package br.com.wdc.store.presentation.presenter.restricted.cart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.store.domain.exception.InvalidCartItemException;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.store.presentation.presenter.restricted.cart.calls.PurchaseCreate;
import br.com.wdc.store.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;

public class CartManager {

    /**
     * What a cart keeps between page reloads: whose it is and how many of each product, in the cart's order. Written as
     * {@code userId|productId:quantity,productId:quantity}; names and prices are read again from the products.
     */
    public record Snapshot(long userId, Map<Long, Integer> quantities) {

        public String format() {
            var sb = new StringBuilder().append(this.userId).append('|');
            var first = true;
            for (var entry : this.quantities.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(entry.getKey()).append(':').append(entry.getValue());
                first = false;
            }
            return sb.toString();
        }

        /** @return the snapshot written by {@link #format()}, or {@code null} if the text is not one */
        public static Snapshot parse(String text) {
            if (text == null || text.isBlank()) {
                return null;
            }
            try {
                var bar = text.indexOf('|');
                var userId = Long.parseLong(text.substring(0, bar));
                var quantities = new LinkedHashMap<Long, Integer>();
                var items = text.substring(bar + 1);
                if (!items.isEmpty()) {
                    for (var item : items.split(",")) {
                        var colon = item.indexOf(':');
                        quantities.put(Long.parseLong(item.substring(0, colon)), Integer.parseInt(item.substring(colon + 1)));
                    }
                }
                return new Snapshot(userId, quantities);
            } catch (RuntimeException caught) {
                return null;
            }
        }
    }

    private final List<CartItem> cart = new ArrayList<>();

    private int listenerIdGen;
    private final Map<Integer, ThrowingRunnable> commitListenerMap = new HashMap<>();
    private final Map<Integer, ThrowingRunnable> changeListenerMap = new HashMap<>();

    public ThrowingRunnable addCommitListener(ThrowingRunnable listener) {
        var listenerID = listenerIdGen++;
        this.commitListenerMap.put(listenerID, listener);
        return () -> this.commitListenerMap.remove(listenerID);
    }

    public ThrowingRunnable addChangeListener(ThrowingRunnable listener) {
        var listenerID = listenerIdGen++;
        this.changeListenerMap.put(listenerID, listener);
        return () -> this.changeListenerMap.remove(listenerID);
    }

    public List<CartItem> getCartItems() {
        return Collections.unmodifiableList(this.cart);
    }

    public void addProduct(ProductInfo product, int quantity) {
        var isNew = true;
        for (var item : this.cart) {
            if (item.id == product.id) {
                item.quantity += quantity;
                isNew = false;
                break;
            }
        }

        if (isNew) {
            this.cart.add(CartItem.create(product, quantity));
        }

        for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
            listener.run();
        }
    }

    public boolean modifyProductQuantity(long productId, int quantity) {
        var found = false;

        var it = this.cart.iterator();
        while (it.hasNext()) {
            var cartItem = it.next();
            if (cartItem.id == productId) {
                cartItem.quantity = quantity;
                found = true;
                break;
            }
        }

        if (found) {
            for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
                listener.run();
            }
        }

        return found;
    }

    public boolean removeProduct(long productId) {
        var modified = false;

        var it = this.cart.iterator();
        while (it.hasNext()) {
            var cartItem = it.next();
            if (cartItem.id == productId) {
                it.remove();
                modified = true;
                break;
            }
        }

        if (modified) {
            for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
                listener.run();
            }
        }

        return modified;
    }

    public Long commit(Subject subject) throws InvalidCartItemException {
        var purchaseId = new PurchaseCreate()
                .withUserId(subject.getId())
                .withItems(this.cart)
                .run();
        this.clear();

        for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
            listener.run();
        }

        for (var listener : new ArrayList<>(this.commitListenerMap.values())) {
            listener.run();
        }
        return purchaseId;
    }


    /** The cart's {@link Snapshot}, for the given owner. */
    public Snapshot snapshot(long userId) {
        var quantities = new LinkedHashMap<Long, Integer>();
        for (var item : this.cart) {
            quantities.put(item.id, item.quantity);
        }
        return new Snapshot(userId, quantities);
    }

    public int getItemCount() {
        var count = 0;
        for (var item : this.cart) {
            count += item.quantity;
        }
        return count;
    }

    public int getSize() {
        return this.cart.size();
    }

    public void clear() {
        this.cart.clear();
    }
}
