package br.com.wdc.shopping.presentation.presenter.restricted.cart.calls;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.shopping.domain.exception.InvalidCartItemException;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.presentation.ShoppingSave;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

/** Records the purchase of the items; answers its id. */
public class PurchaseSave extends ShoppingSave<Long> {

    private Long userId;
    private List<CartItem> items = List.of();

    public PurchaseSave withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public PurchaseSave withItems(List<CartItem> items) {
        this.items = items;
        return this;
    }

    @Override
    protected Long save() {
        var purchase = new Purchase()
                .withUser(new User()
                        .withId(userId))
                .withBuyDate(OffsetDateTime.now());

        purchase.withItems(new ArrayList<>());
        for (var srcItem : items) {
            if (srcItem.quantity < 0) {
                throw new InvalidCartItemException();
            }

            var purchaseItem = new PurchaseItem()
                    .withProduct(new Product()
                            .withId(srcItem.id));
            purchaseItem.withPrice(srcItem.price)
                    .withAmount(srcItem.quantity);

            purchase.items().add(purchaseItem);
        }

        if (!purchaseRepository().insert(purchase)) {
            throw new AssertionError("Record not inserted");
        }
        return purchase.id();
    }

}
