package br.com.wdc.store.persistence.impl.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.store.persistence.impl.scheme.Sequences.SQ_PURCHASE;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnPurchase.EN_PURCHASE;
import static br.com.wdc.store.persistence.impl.scheme.tables.EnPurchaseitem.EN_PURCHASEITEM;

import java.util.Arrays;
import java.util.List;

import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.pagination.Page;
import br.com.wdc.framework.jooq.CriterionTranslator;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.store.domain.purchase.Purchase;
import br.com.wdc.store.domain.purchase.PurchaseCriteria;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.persistence.impl.scheme.tables.EnPurchase;
import br.com.wdc.store.persistence.impl.scheme.tables.EnPurchaseitem;
import br.com.wdc.store.persistence.impl.util.BaseRepositoryImpl;

public class PurchaseRepositoryImpl extends BaseRepositoryImpl  implements PurchaseRepository {

    // @formatter:off
    public static final JsonQuery<Purchase, EnPurchase> QUERY = new JsonQueryBuilder<Purchase, EnPurchase>()
            .setAlias("p")
            .setBeanFactory(Purchase::new)
            .setTableFactory(EN_PURCHASE::as)
            .setDSLContextSupplier(PurchaseRepositoryImpl::dsl)
            .setOrdering(PurchaseRepositoryImpl::orderingOf)
            .addI64("id", p -> p.id(), (p, v) -> p.withId(v), t -> t.ID)
            .addLdt("buyDate", p -> p.buyDate(), (p, v) -> p.withBuyDate(v), t -> t.BUYDATE)
            .lazy(qb -> {
                qb.addBeanField("user", p -> p.user(), (p, v) -> p.withUser(v), UserRepositoryImpl.QUERY, cq -> {
                    var enPurchase = cq.getSuperTable();
                    var enUser = cq.getChildTable();

                    cq.dsl() .where()
                        .and(enUser.ID.eq(enPurchase.USERID))
                        .and(UserRepositoryImpl.applyConditions(cq));
                },
                key -> key.addI64("id", t -> t.USERID));
                
                qb.addBeanListField("items", p -> p.items(), (p, v) -> p.withItems(v), PurchaseItemRepositoryImpl.QUERY, cq -> {
                    var enPurchase = cq.getSuperTable();
                    var enPurchaseItem = cq.getChildTable();
                    
                    cq.dsl().where()
                        .and(enPurchaseItem.PURCHASEID.eq(enPurchase.ID))
                        .and(PurchaseItemRepositoryImpl.applyConditions(cq));
                });
            })
            .build();
    // @formatter:on

    /**
     * Translates the requested ordering into {@code ORDER BY}, against the given table.
     *
     * <p>
     * This is where a provisioned ordering becomes columns: the criteria name the effect, and the choice of columns — and
     * of the tie-breaker — lives in the repository, which is what knows the schema. Every ordering by a non-unique field
     * breaks ties by the key; without that, two runs of the same query could return the rows in different orders.
     * </p>
     *
     * <p>
     * Takes {@code Object} because it is also called from another repository's child collection, where the criteria may
     * not be of this type — in that case it orders nothing.
     * </p>
     */
    public static List<SortField<?>> orderingOf(EnPurchase t, Object criteriaObj) {
        if (!(criteriaObj instanceof PurchaseCriteria criteria) || criteria.orderBy() == null) {
            return List.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST -> List.of(t.ID.asc());
        case NEWEST_FIRST -> List.of(t.ID.desc());
        case MOST_RECENT_PURCHASE_FIRST -> List.of(t.BUYDATE.desc(), t.ID.desc());
        case EARLIEST_PURCHASE_FIRST -> List.of(t.BUYDATE.asc(), t.ID.asc());
        };
    }

    // :: Query helpers

    public static Condition applyConditions(JsonChildQueryBuilder<?, EnPurchase> cq) {
        if (cq.getCriteria() instanceof PurchaseCriteria purchaseCriteria) {
            return new ApplyConditions(cq.getChildTable(), cq.getCtx()).apply(purchaseCriteria);
        }
        return DSL.noCondition();
    }

    private static Condition applyConditions(EnPurchase t, PurchaseCriteria criteria) {
        return new ApplyConditions(t).apply(criteria);
    }

    // :: PurchaseRepository implementation

    @Override
    public boolean insert(Purchase purchase) {
        var dsl = dsl();

        if (purchase.id() == null) {
            purchase.withId(dsl.nextval(SQ_PURCHASE));
        }

        var step = dsl.insertInto(EN_PURCHASE)
                .set(EN_PURCHASE.ID, purchase.id());

        if (purchase.user() != null && purchase.user().id() != null) {
            step.set(EN_PURCHASE.USERID, purchase.user().id());
        }
        if (purchase.buyDate() != null) {
            step.set(EN_PURCHASE.BUYDATE, JsonQueryBuilder.toUtcLdt(purchase.buyDate()));
        }

        var inserted = step.execute() > 0;

        // Insert items if present
        if (inserted && purchase.items() != null && !purchase.items().isEmpty()) {
            for (var item : purchase.items()) {
                item.withPurchase(purchase);
                PurchaseItemRepositoryImpl.insertRow(dsl, item);
            }
        }

        return inserted;
    }


    @Override
    public boolean update(Purchase newBean, Purchase oldBean, Purchase projection) {
        if (newBean == null) {
            throw new InvalidRequestException("update of Purchase without the new state");
        }

        if (newBean.id() == null) {
            throw new InvalidRequestException("update of Purchase without the key (id)");
        }

        if (projection == null) {
            projection = this.newProjection();
        }

        var dsl = dsl();
        var step = dsl.update(EN_PURCHASE).set(EN_PURCHASE.ID, newBean.id());

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, Purchase::userId)) {
            step.set(EN_PURCHASE.USERID, newBean.userId());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, p -> p.buyDate())) {
            step.set(EN_PURCHASE.BUYDATE, JsonQueryBuilder.toUtcLdt(newBean.buyDate()));
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_PURCHASE.ID.eq(newBean.id())).execute() > 0;
    }

    @Override
    public int delete(PurchaseCriteria criteria) {
        var dsl = dsl();
        var condition = applyConditions(EN_PURCHASE, criteria);

        // Delete child items first (for all matching purchases)
        dsl.deleteFrom(EN_PURCHASEITEM)
                .where(EN_PURCHASEITEM.PURCHASEID.in(
                        DSL.select(EN_PURCHASE.ID).from(EN_PURCHASE).where(condition)))
                .execute();

        return dsl.deleteFrom(EN_PURCHASE)
                .where(condition)
                .execute();
    }

    @Override
    public int count(PurchaseCriteria criteria) {
        return dsl().selectCount()
                .from(EN_PURCHASE)
                .where(applyConditions(EN_PURCHASE, criteria))
                .fetchOne()
                .value1();
    }

    @Override
    public List<Purchase> fetch(PurchaseCriteria criteria, int offset, int limit) {
        var prjBean = projectionFrom(criteria);

        return QUERY.fetchToList(prjBean, (t, q) -> {
            var cond = applyConditions(t, criteria);
            var step = q.where(cond);

            step.orderBy(orderingOf(t, criteria));

            if (limit > 0) {
                step.limit(limit);
            }
            if (offset > 0) {
                step.offset(offset);
            }
        });
    }

    @Override
    public Page<Purchase> fetchPage(PurchaseCriteria criteria, int page, int pageSize) {
        var total = count(criteria);
        var items = fetch(criteria, page * pageSize, pageSize);
        return Page.of(items, page, pageSize, total);
    }


    // :: Internal

    private Purchase projectionFrom(PurchaseCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            if (prj.id() == null) {
                prj.withId(0L);
            }
            return prj;
        }
        return this.newProjection();
    }

    private static class ApplyConditions {

        EnPurchase enPurchase;
        QueryContext ctx;

        ApplyConditions(EnPurchase t, QueryContext ctx) {
            this.enPurchase = t;
            this.ctx = ctx;
        }

        ApplyConditions(EnPurchase t) {
            this.enPurchase = t;
            this.ctx = new QueryContext(dsl());
        }

        /**
         * The conditions for the fields that are set.
         *
         * <p>
         * Walking the fields and assembling the {@code AND} live in {@link CriterionTranslator}; here is only what is
         * specific to the entity — the column of each field and, for {@code productId}, the fact that it is not a column
         * of this table. Empty criteria result in {@code noCondition()}, not in a false condition.
         * </p>
         */
        public Condition apply(PurchaseCriteria criteria) {
            if (criteria == null) {
                return DSL.noCondition();
            }
            return CriterionTranslator.and(Arrays.asList(
                    CriterionTranslator.translate(enPurchase.ID, criteria.purchaseId()),
                    CriterionTranslator.translate(enPurchase.USERID, criteria.userId()),
                    // The column is TIMESTAMP without time zone; the domain speaks OffsetDateTime. Converting here keeps
                    // the comparison in the same type on both sides.
                    CriterionTranslator.translate(enPurchase.BUYDATE, criteria.buyDate(), JsonQueryBuilder::toUtcLdt),
                    existsItemMatching(criteria),
                    CriterionTranslator.anyOf(criteria.anyOf(), this::apply)));
        }

        /**
         * Purchases that contain the requested product.
         *
         * <p>
         * The product is not a column of the purchase — it lives in the items. The criterion, however, is still a field
         * like the others: what changes is where the condition applies, not how it is written, so {@code in},
         * {@code between} and the disjunction work here just the same.
         * </p>
         */
        private Condition existsItemMatching(PurchaseCriteria criteria) {
            if (!criteria.hasProductId()) {
                return null;
            }
            var enPurchaseItem = EnPurchaseitem.EN_PURCHASEITEM.as(ctx.alias("p"));
            var onProduct = CriterionTranslator.translate(enPurchaseItem.PRODUCTID, criteria.productId());
            if (onProduct == null) {
                return null;
            }
            return DSL.exists(DSL.selectOne()
                    .from(enPurchaseItem)
                    .where(enPurchaseItem.PURCHASEID.eq(enPurchase.ID))
                    .and(onProduct));
        }

    }
}
