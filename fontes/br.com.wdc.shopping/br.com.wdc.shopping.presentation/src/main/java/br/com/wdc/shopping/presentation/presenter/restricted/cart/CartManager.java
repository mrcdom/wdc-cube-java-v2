package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.shopping.domain.exception.InvalidCartItemException;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class CartManager {

    private final ShoppingApplication app;
    private List<CartItem> cart;

    private int listenerIdGen;
    private Map<Integer, ThrowingRunnable> commitListenerMap;
    private Map<Integer, ThrowingRunnable> changeListenerMap;

    public CartManager(ShoppingApplication app) {
        this.app = app;
        this.cart = new ArrayList<>();
        this.commitListenerMap = new HashMap<>();
        this.changeListenerMap = new HashMap<>();
    }

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

            for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
                listener.run();
            }
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
        var purchaseId = this.doPurchase(subject.getId(), this.cart);
        this.clear();

        for (var listener : new ArrayList<>(this.changeListenerMap.values())) {
            listener.run();
        }

        for (var listener : new ArrayList<>(this.commitListenerMap.values())) {
            listener.run();
        }
        return purchaseId;
    }

    private Long doPurchase(Long userId, List<CartItem> request) {
        var purchase = new Purchase();
        purchase.user = new User();
        purchase.user.id = userId;
        purchase.buyDate = OffsetDateTime.now();

        purchase.items = new ArrayList<>();
        for (var srcItem : request) {
            if (srcItem.quantity < 0) {
                throw new InvalidCartItemException();
            }

            var purchaseItem = new PurchaseItem();
            purchaseItem.product = new Product();
            purchaseItem.product.id = srcItem.id;
            purchaseItem.price = srcItem.price;
            purchaseItem.amount = srcItem.quantity;

            purchase.items.add(purchaseItem);
        }

        if (!app.getPurchaseRepository().insert(purchase)) {
            throw new AssertionError("Record not inserted");
        }

        return purchase.id;
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
