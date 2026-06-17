package br.com.wdc.shopping.persistence.impl.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.shopping.persistence.impl.jooq.Sequences.SQ_PURCHASE;
import static br.com.wdc.shopping.persistence.impl.jooq.Sequences.SQ_PURCHASEITEM;
import static br.com.wdc.shopping.persistence.impl.jooq.tables.EnPurchase.EN_PURCHASE;
import static br.com.wdc.shopping.persistence.impl.jooq.tables.EnPurchaseitem.EN_PURCHASEITEM;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.framework.domain.pagination.Page;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.persistence.impl.jooq.tables.EnPurchase;
import br.com.wdc.shopping.persistence.impl.jooq.tables.EnPurchaseitem;

public class PurchaseRepositoryImpl implements PurchaseRepository {

    // @formatter:off
    public static final JsonQuery<Purchase, EnPurchase> QUERY = new JsonQueryBuilder<Purchase, EnPurchase>()
            .setAlias("p")
            .setBeanFactory(Purchase::new)
            .setTableFactory(EN_PURCHASE::as)
            .addI64("id", p -> p.id, (p, v) -> p.id = v, t -> t.ID)
            .addLdt("buyDate", p -> p.buyDate, (p, v) -> p.buyDate = v, t -> t.BUYDATE)
            .lazy(qb -> {
                qb.addBeanField("user", p -> p.user, (p, v) -> p.user = v, UserRepositoryImpl.QUERY, cq -> {
                    var enPurchase = cq.getSuperTable();
                    var enUser = cq.getChildTable();

                    cq.dsl() .where()
                        .and(enUser.ID.eq(enPurchase.USERID))
                        .and(UserRepositoryImpl.applyConditions(cq));
                });
                
                qb.addBeanListField("items", p -> p.items, (p, v) -> p.items = v, PurchaseItemRepositoryImpl.QUERY, cq -> {
                    var enPurchase = cq.getSuperTable();
                    var enPurchaseItem = cq.getChildTable();
                    
                    cq.dsl().where()
                        .and(enPurchaseItem.PURCHASEID.eq(enPurchase.ID))
                        .and(PurchaseItemRepositoryImpl.applyConditions(cq));
                });
            })
            .build();
    // @formatter:on

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
        var dsl = JooqDSLContext.BEAN.get();

        if (purchase.id == null) {
            purchase.id = dsl.nextval(SQ_PURCHASE);
        }

        var step = dsl.insertInto(EN_PURCHASE)
                .set(EN_PURCHASE.ID, purchase.id);

        if (purchase.user != null && purchase.user.id != null) {
            step.set(EN_PURCHASE.USERID, purchase.user.id);
        }
        if (purchase.buyDate != null) {
            step.set(EN_PURCHASE.BUYDATE, purchase.buyDate.toLocalDateTime());
        }

        var inserted = step.execute() > 0;

        // Insert items if present
        if (inserted && purchase.items != null && !purchase.items.isEmpty()) {
            for (var item : purchase.items) {
                item.purchase = purchase;
                insertItem(dsl, item);
            }
        }

        return inserted;
    }

    private void insertItem(org.jooq.DSLContext dsl, PurchaseItem item) {
        if (item.id == null) {
            item.id = dsl.nextval(SQ_PURCHASEITEM);
        }

        var step = dsl.insertInto(EN_PURCHASEITEM)
                .set(EN_PURCHASEITEM.ID, item.id);

        if (item.purchase != null && item.purchase.id != null) {
            step.set(EN_PURCHASEITEM.PURCHASEID, item.purchase.id);
        }
        if (item.product != null && item.product.id != null) {
            step.set(EN_PURCHASEITEM.PRODUCTID, item.product.id);
        }
        if (item.amount != null) {
            step.set(EN_PURCHASEITEM.AMOUNT, item.amount);
        }
        if (item.price != null) {
            step.set(EN_PURCHASEITEM.PRICE, BigDecimal.valueOf(item.price));
        }

        step.execute();
    }

    @Override
    public boolean update(Purchase newBean, Purchase oldBean, Purchase projection) {
        if (newBean == null) {
            throw new AssertionError("newBean is required");
        }

        if (newBean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        if (projection == null) {
            projection = this.newProjection();
        }

        var dsl = JooqDSLContext.BEAN.get();
        var step = dsl.update(EN_PURCHASE).set(EN_PURCHASE.ID, newBean.id);

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, Purchase::userId)) {
            step.set(EN_PURCHASE.USERID, newBean.userId());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, p -> p.buyDate)) {
            step.set(EN_PURCHASE.BUYDATE, newBean.buyDate != null ? newBean.buyDate.toLocalDateTime() : null);
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_PURCHASE.ID.eq(newBean.id)).execute() > 0;
    }

    @Override
    public int delete(PurchaseCriteria criteria) {
        var dsl = JooqDSLContext.BEAN.get();
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
        var dsl = JooqDSLContext.BEAN.get();
        return dsl.selectCount()
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

            if (criteria != null && criteria.orderBy() != null) {
                switch (criteria.orderBy()) {
                case ASCENDING -> step.orderBy(t.ID.asc());
                case DESCENDING -> step.orderBy(t.ID.desc());
                }
            }

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

    @Override
    public Purchase fetchById(Long purchaseId, Purchase projection) {
        var prjBean = projection != null ? projection : QUERY.newProjectionBean();
        if (prjBean.id == null) {
            prjBean.id = 0L;
        }

        return QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(purchaseId)));
    }

    // :: Internal

    private Purchase projectionFrom(PurchaseCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            if (prj.id == null) {
                prj.id = 0L;
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
            this.ctx = new QueryContext();
        }

        public Condition apply(PurchaseCriteria criteria) {
            var condition = DSL.noCondition();
            if (criteria == null) {
                return condition;
            }
            if (criteria.purchaseId() != null) {
                condition = condition.and(enPurchase.ID.eq(criteria.purchaseId()));
            }
            if (criteria.userId() != null) {
                condition = condition.and(enPurchase.USERID.eq(criteria.userId()));
            }
            if (criteria.productId() != null) {
                var enPurchaseItem = EnPurchaseitem.EN_PURCHASEITEM.as(ctx.alias("p"));

                condition = condition.and(DSL.exists(DSL.selectOne()
                        .from(enPurchaseItem)
                        .where()
                        .and(enPurchaseItem.PURCHASEID.eq(enPurchase.ID))
                        .and(enPurchaseItem.PRODUCTID.eq(criteria.productId()))));

            }
            return condition;
        }

    }
}
