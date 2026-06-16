package br.com.wdc.shopping.persistence.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_PURCHASEITEM;
import static br.com.wdc.shopping.persistence.jooq.tables.EnPurchase.EN_PURCHASE;
import static br.com.wdc.shopping.persistence.jooq.tables.EnPurchaseitem.EN_PURCHASEITEM;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.persistence.jooq.tables.EnPurchaseitem;

public class PurchaseItemRepositoryImpl implements PurchaseItemRepository {

    // @formatter:off
    public static final JsonQuery<PurchaseItem, EnPurchaseitem> QUERY = new JsonQueryBuilder<PurchaseItem, EnPurchaseitem>()
            .setAlias("pi")
            .setBeanFactory(PurchaseItem::new)
            .setTableFactory(EN_PURCHASEITEM::as)
            .addI64("id", pi -> pi.id, (pi, v) -> pi.id = v, t -> t.ID)
            .addI32("amount", pi -> pi.amount, (pi, v) -> pi.amount = v, t -> t.AMOUNT)
            .addF64("price", pi -> pi.price, (pi, v) -> pi.price = v, t -> t.PRICE)
            .lazy(qb -> { qb
            	.addBeanField("purchase", pi -> pi.purchase, (pi, v) -> pi.purchase = v, PurchaseRepositoryImpl.QUERY, cq -> {
            		var enPurchaseItem = cq.getSuperTable();
            		var enPurchase = cq.getChildTable();
            		
            		cq.dsl().where()
            			.and(enPurchase.ID.eq(enPurchaseItem.PURCHASEID))
            			.and(PurchaseRepositoryImpl.applyConditions(cq));
            	})
            	.addBeanField("product", pi -> pi.product, (pi, v) -> pi.product = v, ProductRepositoryImpl.QUERY, cq -> {
            		var enPurchaseItem = cq.getSuperTable();
            		var enProduct = cq.getChildTable();
            		
            		cq.dsl().where()
            			.and(enProduct.ID.eq(enPurchaseItem.PRODUCTID))
            			.and(ProductRepositoryImpl.applyConditions(cq));
            	});
            })
            .build();
    // @frmatter:on

    // :: Query helpers
    
    public static Condition applyConditions(JsonChildQueryBuilder<?, EnPurchaseitem> cq) {
    	if (cq.getCriteria() instanceof PurchaseItemCriteria purchaseItemCriteria) {
    	    return new ApplyConditions(cq.getChildTable(), cq.getCtx()).apply(purchaseItemCriteria);
    	}
    	return DSL.noCondition();
    }

    private static Condition applyConditions(EnPurchaseitem t, PurchaseItemCriteria criteria) {
        return new ApplyConditions(t).apply(criteria);
    }

    // :: PurchaseItemRepository implementation

    @Override
    public boolean insert(PurchaseItem item) {
        var dsl = JooqDSLContext.BEAN.get();

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

        return step.execute() > 0;
    }

    @Override
    public boolean update(PurchaseItem newBean, PurchaseItem oldBean, PurchaseItem projection) {
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
        var step = dsl.update(EN_PURCHASEITEM).set(EN_PURCHASEITEM.ID, newBean.id);

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, PurchaseItem::purchaseId)) {
            step.set(EN_PURCHASEITEM.PURCHASEID, newBean.productId());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, PurchaseItem::productId)) {
            step.set(EN_PURCHASEITEM.PRODUCTID, newBean.productId());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, pi -> pi.amount)) {
            step.set(EN_PURCHASEITEM.AMOUNT, newBean.amount);
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, pi -> pi.price)) {
            step.set(EN_PURCHASEITEM.PRICE, newBean.price != null ? BigDecimal.valueOf(newBean.price) : null);
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_PURCHASEITEM.ID.eq(newBean.id)).execute() > 0;
    }

    @Override
    public int delete(PurchaseItemCriteria criteria) {
        var dsl = JooqDSLContext.BEAN.get();
        return dsl.deleteFrom(EN_PURCHASEITEM)
                .where(applyConditions(EN_PURCHASEITEM, criteria))
                .execute();
    }

    @Override
    public int count(PurchaseItemCriteria criteria) {
        var dsl = JooqDSLContext.BEAN.get();
        return dsl.selectCount()
                .from(EN_PURCHASEITEM)
                .where(applyConditions(EN_PURCHASEITEM, criteria))
                .fetchOne()
                .value1();
    }

    @Override
    public List<PurchaseItem> fetch(PurchaseItemCriteria criteria, int offset, int limit) {
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
    public PurchaseItem fetchById(Long purchaseItemId, PurchaseItem projection) {
        var prjBean = projection != null ? projection : QUERY.newProjectionBean();
        if (prjBean.id == null) {
            prjBean.id = 0L;
        }

        return QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(purchaseItemId)));
    }

    // :: Internal

    private PurchaseItem projectionFrom(PurchaseItemCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            if (prj.id == null) {
                prj.id = 0L;
            }
            return prj;
        }
        return this.newProjection();
    }
    
    static class ApplyConditions {

        EnPurchaseitem enPurchaseItem;
        QueryContext ctx;

        ApplyConditions(EnPurchaseitem t, QueryContext ctx) {
            this.enPurchaseItem = t;
            this.ctx = ctx;
        }

        ApplyConditions(EnPurchaseitem t) {
            this.enPurchaseItem = t;
            this.ctx = new QueryContext();
        }

        public Condition apply(PurchaseItemCriteria criteria) {
            var condition = DSL.noCondition();
            if (criteria == null) {
                return condition;
            }
            if (criteria.purchaseItemId() != null) {
                condition = condition.and(this.enPurchaseItem.ID.eq(criteria.purchaseItemId()));
            }
            if (criteria.purchaseId() != null) {
                condition = condition.and(this.enPurchaseItem.PURCHASEID.eq(criteria.purchaseId()));
            }
            if (criteria.productId() != null) {
                condition = condition.and(this.enPurchaseItem.PRODUCTID.eq(criteria.productId()));
            }
            if (criteria.userId() != null) {
                // userId filter requires correlation with EN_PURCHASE
                condition = condition.and(DSL.exists(
                        DSL.selectOne()
                                .from(EN_PURCHASE)
                                .where(EN_PURCHASE.ID.eq(this.enPurchaseItem.PURCHASEID)
                                        .and(EN_PURCHASE.USERID.eq(criteria.userId())))));
            }
            return condition;
        }
    }
}
