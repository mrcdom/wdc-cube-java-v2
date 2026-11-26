package br.com.wdc.store.persistence.rest.security;

import java.util.Set;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.DataPolicy;
import br.com.wdc.framework.domain.security.policy.EntityDescriptor;
import br.com.wdc.framework.domain.security.policy.RowScope;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.domain.security.Role;
import br.com.wdc.store.domain.user.User;

/**
 * Who may do what with the purchase items through the REST API.
 *
 * <ul>
 * <li><b>ADMIN</b> reads every item, creates, updates and deletes them. A price it sends is replaced by the product's
 * price, by the repository.</li>
 * <li><b>CUSTOMER</b> reads the items of its own purchases, and creates items in them — without a price: the price
 * of an item is the price of its product when it is created, filled in by the repository.</li>
 * <li><b>MANAGER</b> reads the items of its own purchases.</li>
 * </ul>
 *
 * <p>
 * What a read asks inside {@code purchase} goes through {@link PurchaseDataPolicy}, and inside {@code product} through
 * {@link ProductDataPolicy}.
 * </p>
 */
public final class PurchaseItemDataPolicy {

    private static final ProjectionValues PV = ProjectionValues.INSTANCE;

    private PurchaseItemDataPolicy() {
    }

    // @formatter:off
    public static final EntityDescriptor<PurchaseItem, PurchaseItemCriteria> PURCHASE_ITEM = EntityDescriptor.<PurchaseItem, PurchaseItemCriteria>builder("purchase-item")
            .entity(PurchaseItem::new)
            .criteria(PurchaseItemCriteria::new)
            .projection(PurchaseItemCriteria::projection, PurchaseItemCriteria::withProjection)
            .anyOf(PurchaseItemCriteria::anyOf, PurchaseItemCriteria::withAnyOf)
            .key("id", PurchaseItem::id, PurchaseItem::withId, PV.i64)
            .field("amount", PurchaseItem::amount, PurchaseItem::withAmount, PV.i32)
            .field("price", PurchaseItem::price, PurchaseItem::withPrice, PV.f64)
            .relation("purchase", PurchaseItem::purchase, PurchaseItem::withPurchase, () -> PurchaseDataPolicy.POLICY)
            .relation("product", PurchaseItem::product, PurchaseItem::withProduct, () -> ProductDataPolicy.POLICY)
            .criterion("purchaseItemId", "id")
            .criterion("purchaseId", "purchase")
            .criterion("productId", "product")
            .criterion("userId", "purchase")
            .orderings(PurchaseItemDataPolicy::orderingFields)
            .defaultShape(PurchaseItemDataPolicy::defaultShape)
            .build();
    // @formatter:on

    /**
     * The items of the requester's own purchases.
     *
     * <p>
     * A read fetches {@code purchase.user} for the check. A new item may come with its purchase by key only — sent on
     * its own to {@code purchase-item/insert} —, and then the owner of that purchase is looked up.
     * </p>
     */
    public static final RowScope<PurchaseItem, PurchaseItemCriteria> OWN = new RowScope<>() {

        @Override
        public void restrict(PurchaseItemCriteria group, SecurityContext sc) {
            group.withUserId(sc.userId());
        }

        @Override
        public boolean covers(PurchaseItem row, SecurityContext sc) {
            var purchase = row.purchase();
            if (purchase == null) {
                return false;
            }
            var user = purchase.user();
            if (user == null && purchase.id() != null) {
                var stored = PurchaseRepository.BEAN.get().fetchById(purchase.id(), new Purchase()
                        .withId(PV.i64)
                        .withUser(new User()
                                .withId(PV.i64)));
                user = stored == null ? null : stored.user();
            }
            return user != null && sc.userId().equals(user.id());
        }

        @Override
        public PurchaseItem projection() {
            return new PurchaseItem()
                    .withPurchase(new Purchase()
                            .withUser(new User()
                                    .withId(PV.i64)));
        }

        @Override
        public String toString() {
            return "OWN";
        }
    };

    public static final DataPolicy<PurchaseItem, PurchaseItemCriteria> POLICY = DataPolicy.on(PURCHASE_ITEM)
            .grant(Role.ADMIN.name(), g -> g
                    .read(whole())
                    .create(whole())
                    .update(whole())
                    .delete())
            .grant(Role.CUSTOMER.name(), g -> g
                    .rows(OWN)
                    .read(whole())
                    .create(new PurchaseItem()
                            .withAmount(PV.i32)
                            .withPurchase(new Purchase())
                            .withProduct(new Product())))
            .grant(Role.MANAGER.name(), g -> g
                    .rows(OWN)
                    .read(whole()))
            .build();

    private static PurchaseItem whole() {
        return new PurchaseItem()
                .withAmount(PV.i32)
                .withPrice(PV.f64)
                .withPurchase(new Purchase())
                .withProduct(new Product());
    }

    /** A read without a projection: quantity, price and the product's name and price — also a receipt's line. */
    static PurchaseItem defaultShape() {
        return new PurchaseItem()
                .withAmount(PV.i32)
                .withPrice(PV.f64)
                .withProduct(new Product()
                        .withName(PV.str)
                        .withPrice(PV.f64));
    }

    /** The fields each provisioned ordering sorts by; the tie-breaker is the key, which is not checked. */
    private static Set<String> orderingFields(PurchaseItemCriteria criteria) {
        if (criteria.orderBy() == null) {
            return Set.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST, NEWEST_FIRST -> Set.of();
        case MOST_EXPENSIVE_FIRST -> Set.of("price");
        case LARGEST_QUANTITY_FIRST -> Set.of("amount");
        };
    }
}
