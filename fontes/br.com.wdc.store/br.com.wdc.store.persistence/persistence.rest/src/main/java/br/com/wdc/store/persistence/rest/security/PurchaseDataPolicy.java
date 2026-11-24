package br.com.wdc.store.persistence.rest.security;

import java.util.List;
import java.util.Set;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.DataPolicy;
import br.com.wdc.framework.domain.security.policy.EntityDescriptor;
import br.com.wdc.framework.domain.security.policy.RowScope;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.security.Role;
import br.com.wdc.store.domain.user.User;

/**
 * Who may do what with the purchases through the REST API. A purchase is a receipt: once created, only an ADMIN
 * changes it.
 *
 * <ul>
 * <li><b>ADMIN</b> reads every purchase, creates, updates and deletes them.</li>
 * <li><b>CUSTOMER</b> reads and creates its own purchases.</li>
 * <li><b>MANAGER</b> reads its own purchases.</li>
 * </ul>
 *
 * <p>
 * What a read asks inside {@code user} goes through {@link UserDataPolicy}, and inside {@code items} through
 * {@link PurchaseItemDataPolicy}; the items of a new purchase are created through the latter, as items of that
 * purchase.
 * </p>
 */
public final class PurchaseDataPolicy {

    private static final ProjectionValues PV = ProjectionValues.INSTANCE;

    private PurchaseDataPolicy() {
    }

    // @formatter:off
    public static final EntityDescriptor<Purchase, PurchaseCriteria> PURCHASE = EntityDescriptor.<Purchase, PurchaseCriteria>builder("purchase")
            .entity(Purchase::new)
            .criteria(PurchaseCriteria::new)
            .projection(PurchaseCriteria::projection, PurchaseCriteria::withProjection)
            .anyOf(PurchaseCriteria::anyOf, PurchaseCriteria::withAnyOf)
            .key("id", Purchase::id, Purchase::withId, PV.i64)
            .field("buyDate", Purchase::buyDate, Purchase::withBuyDate, PV.offsetDateTime)
            .relation("user", Purchase::user, Purchase::withUser, () -> UserDataPolicy.POLICY)
            .collection("items", Purchase::items, Purchase::withItems, () -> PurchaseItemDataPolicy.POLICY, "purchase")
            .criterion("purchaseId", "id")
            .criterion("userId", "user")
            .criterion("productId", "items")
            .orderings(PurchaseDataPolicy::orderingFields)
            .build();
    // @formatter:on

    /** The requester's own purchases. */
    public static final RowScope<Purchase, PurchaseCriteria> OWN = new RowScope<>() {

        @Override
        public void restrict(PurchaseCriteria group, SecurityContext sc) {
            group.withUserId(sc.userId());
        }

        @Override
        public boolean covers(Purchase row, SecurityContext sc) {
            return row.user() != null && sc.userId().equals(row.user().id());
        }

        @Override
        public Purchase projection() {
            return new Purchase()
                    .withUser(new User()
                            .withId(PV.i64));
        }

        @Override
        public String toString() {
            return "OWN";
        }
    };

    public static final DataPolicy<Purchase, PurchaseCriteria> POLICY = DataPolicy.on(PURCHASE)
            .grant(Role.ADMIN.name(), g -> g
                    .read(receipt())
                    .create(receipt())
                    .update(new Purchase()
                            .withBuyDate(PV.offsetDateTime)
                            .withUser(new User()))
                    .delete())
            .grant(Role.CUSTOMER.name(), g -> g
                    .rows(OWN)
                    .read(receipt())
                    .create(receipt()))
            .grant(Role.MANAGER.name(), g -> g
                    .rows(OWN)
                    .read(receipt()))
            .build();

    /** A purchase as a whole: its date, its buyer and its items. */
    private static Purchase receipt() {
        return new Purchase()
                .withBuyDate(PV.offsetDateTime)
                .withUser(new User())
                .withItems(List.of());
    }

    /** The fields each provisioned ordering sorts by; the tie-breaker is the key, which is not checked. */
    private static Set<String> orderingFields(PurchaseCriteria criteria) {
        if (criteria.orderBy() == null) {
            return Set.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST, NEWEST_FIRST -> Set.of();
        case MOST_RECENT_PURCHASE_FIRST, EARLIEST_PURCHASE_FIRST -> Set.of("buyDate");
        };
    }
}
