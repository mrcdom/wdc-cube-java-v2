package br.com.wdc.store.presentation.presenter.restricted.cart.calls;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.domain.resource.ResourceCreate;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.domain.exception.InvalidCartItemException;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.presentation.presenter.restricted.cart.structs.CartItem;

/** Records the purchase of the items; answers its id. */
public class PurchaseCreate extends ResourceCreate<Long> {

    private Long userId;
    private List<CartItem> items = List.of();

    public PurchaseCreate() {
        super(StoreTransactions.BEAN::get);
    }

    public PurchaseCreate withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public PurchaseCreate withItems(List<CartItem> items) {
        this.items = items;
        return this;
    }

    @Override
    protected Long create() {
        var purchase = new Purchase()
                .withUser(new User()
                        .withId(userId))
                .withBuyDate(OffsetDateTime.now());

        purchase.withItems(new ArrayList<>());
        for (var srcItem : items) {
            if (srcItem.quantity < 0) {
                throw new InvalidCartItemException();
            }

            // No price: the item costs what its product costs when the purchase is recorded, and the repository
            // fills it in — the cart's price is what the screen showed, not what is charged.
            purchase.items().add(new PurchaseItem()
                    .withProduct(new Product()
                            .withId(srcItem.id))
                    .withAmount(srcItem.quantity));
        }

        if (!PurchaseRepository.BEAN.get().insert(purchase)) {
            throw new AssertionError("Record not inserted");
        }
        return purchase.id();
    }

}
