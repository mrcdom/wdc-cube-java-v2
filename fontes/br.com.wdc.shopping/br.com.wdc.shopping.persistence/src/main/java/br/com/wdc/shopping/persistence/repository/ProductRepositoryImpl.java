package br.com.wdc.shopping.persistence.repository;

import static br.com.wdc.shopping.persistence.jooq.Sequences.SQ_PRODUCT;
import static br.com.wdc.shopping.persistence.jooq.tables.EnProduct.EN_PRODUCT;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.persistence.jooq.tables.EnProduct;

public class ProductRepositoryImpl implements ProductRepository {

    // @formatter:off
    public static final JsonQuery<Product, EnProduct> QUERY = new JsonQueryBuilder<Product, EnProduct>()
            .setAlias("p")
            .setBeanFactory(Product::new)
            .setTableFactory(EN_PRODUCT::as)
            .addI64("id", p -> p.id, (p, v) -> p.id = v, t -> t.ID)
            .addStr("name", p -> p.name, (p, v) -> p.name = v, t -> t.NAME)
            .addF64("price", p -> p.price, (p, v) -> p.price = v, t -> t.PRICE)
            .addStr("description", p -> p.description, (p, v) -> p.description = v, t -> t.DESCRIPTION)
            .addBin("image", p -> p.image, (p, v) -> p.image = v, t -> t.IMAGE)
            .build();
    // @formatter:on

    // :: Query helpers

    public static Condition applyConditions(JsonChildQueryBuilder<?, EnProduct> cq) {
        if (cq.getCriteria() instanceof ProductCriteria productCriteria) {
            return new ApplyConditions(cq.getChildTable(), cq.getCtx()).apply(productCriteria);
        }
        return DSL.noCondition();
    }

    private static Condition applyConditions(EnProduct t, ProductCriteria criteria) {
        return new ApplyConditions(t).apply(criteria);
    }

    // :: ProductRepository implementation

    @Override
    public boolean insert(Product product) {
        var dsl = JooqDSLContext.BEAN.get();

        if (product.id == null) {
            product.id = dsl.nextval(SQ_PRODUCT);
        }

        var step = dsl.insertInto(EN_PRODUCT)
                .set(EN_PRODUCT.ID, product.id);

        if (product.name != null) {
            step.set(EN_PRODUCT.NAME, product.name);
        }
        if (product.price != null) {
            step.set(EN_PRODUCT.PRICE, BigDecimal.valueOf(product.price));
        }
        if (product.description != null) {
            step.set(EN_PRODUCT.DESCRIPTION, product.description);
        }
        if (product.image != null) {
            step.set(EN_PRODUCT.IMAGE, product.image);
        }

        var inserted = step.execute() > 0;

        return inserted;
    }

    @Override
    public boolean update(Product newBean, Product oldBean) {
        if (newBean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        var dsl = JooqDSLContext.BEAN.get();
        var step = dsl.update(EN_PRODUCT).set(EN_PRODUCT.ID, newBean.id);

        boolean hasChanges = false;

        if (newBean.name != null) {
            step.set(EN_PRODUCT.NAME, newBean.name);
            hasChanges = true;
        }
        if (newBean.price != null) {
            step.set(EN_PRODUCT.PRICE, BigDecimal.valueOf(newBean.price));
            hasChanges = true;
        }
        if (newBean.description != null) {
            step.set(EN_PRODUCT.DESCRIPTION, newBean.description);
            hasChanges = true;
        }
        if (!hasChanges && newBean.image == null) {
            return false;
        }

        if (newBean.image != null) {
            step.set(EN_PRODUCT.IMAGE, newBean.image);
        }

        step.where(EN_PRODUCT.ID.eq(newBean.id)).execute();

        return true;
    }

    @Override
    public int delete(ProductCriteria criteria) {
        if (criteria == null || criteria.productId() == null) {
            throw new AssertionError("Missing primary key");
        }

        var dsl = JooqDSLContext.BEAN.get();
        return dsl.deleteFrom(EN_PRODUCT)
                .where(applyConditions(EN_PRODUCT, criteria))
                .execute();
    }

    @Override
    public int count(ProductCriteria criteria) {
        var dsl = JooqDSLContext.BEAN.get();
        return dsl.selectCount()
                .from(EN_PRODUCT)
                .where(applyConditions(EN_PRODUCT, criteria))
                .fetchOne()
                .value1();
    }

    @Override
    public List<Product> fetch(ProductCriteria criteria, int offset, int limit) {
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
    public Product fetchById(Long productId, Product projection) {
        var prjBean = projection != null ? projection : QUERY.newProjectionBean();
        // Ensure id is always projected
        if (prjBean.id == null) {
            prjBean.id = 0L;
        }

        return QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(productId)));
    }

    @Override
    public byte[] fetchImage(Long productId) {
        var prjBean = new Product();
        prjBean.id = 0L;
        prjBean.image = new byte[0];

        var product = QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(productId)));
        if (product == null) {
            throw new BusinessException("Product not found: " + productId);
        }
        return product.image;
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        var dsl = JooqDSLContext.BEAN.get();
        return dsl.update(EN_PRODUCT)
                .set(EN_PRODUCT.IMAGE, image)
                .where(EN_PRODUCT.ID.eq(productId))
                .execute() > 0;
    }

    // :: Internal

    private Product projectionFrom(ProductCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            // Always include id
            if (prj.id == null) {
                prj.id = 0L;
            }
            return prj;
        }
        return this.newProjection();
    }

    static class ApplyConditions {

        EnProduct enProduct;
        QueryContext ctx;

        ApplyConditions(EnProduct t, QueryContext ctx) {
            this.enProduct = t;
            this.ctx = ctx;
        }

        ApplyConditions(EnProduct t) {
            this.enProduct = t;
            this.ctx = new QueryContext();
        }

        public Condition apply(ProductCriteria criteria) {
            var condition = DSL.noCondition();
            if (criteria != null && criteria.productId() != null) {
                condition = condition.and(enProduct.ID.eq(criteria.productId()));
            }
            return condition;
        }
    }
}
