package br.com.wdc.shopping.persistence.impl.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.shopping.persistence.impl.scheme.Sequences.SQ_PRODUCT;
import static br.com.wdc.shopping.persistence.impl.scheme.Tables.EN_PRODUCT;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.jooq.Condition;
import org.jooq.SortField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.jooq.CriterionTranslator;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.persistence.impl.scheme.tables.EnProduct;
import br.com.wdc.shopping.persistence.impl.util.BaseRepositoryImpl;

public class ProductRepositoryImpl extends BaseRepositoryImpl implements ProductRepository {
    
    // @formatter:off
    public static final JsonQuery<Product, EnProduct> QUERY = new JsonQueryBuilder<Product, EnProduct>()
            .setAlias("p")
            .setBeanFactory(Product::new)
            .setTableFactory(EN_PRODUCT::as)
            .setDSLContextSupplier(ProductRepositoryImpl::dsl)
            .setOrdering(ProductRepositoryImpl::orderingOf)
            .addI64("id", p -> p.id(), (p, v) -> p.withId(v), t -> t.ID)
            .addStr("name", p -> p.name(), (p, v) -> p.withName(v), t -> t.NAME)
            .addF64("price", p -> p.price(), (p, v) -> p.withPrice(v), t -> t.PRICE)
            .addStr("description", p -> p.description(), (p, v) -> p.withDescription(v), t -> t.DESCRIPTION)
            .addBin("image", p -> p.image(), (p, v) -> p.withImage(v), t -> t.IMAGE)
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
    public static List<SortField<?>> orderingOf(EnProduct t, Object criteriaObj) {
        if (!(criteriaObj instanceof ProductCriteria criteria) || criteria.orderBy() == null) {
            return List.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST -> List.of(t.ID.asc());
        case NEWEST_FIRST -> List.of(t.ID.desc());
        case NAME_A_TO_Z -> List.of(t.NAME.asc(), t.ID.asc());
        case CHEAPEST_FIRST -> List.of(t.PRICE.asc(), t.ID.asc());
        case MOST_EXPENSIVE_FIRST -> List.of(t.PRICE.desc(), t.ID.asc());
        };
    }

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
        var dsl = dsl();

        if (product.id() == null) {
            product.withId(dsl.nextval(SQ_PRODUCT));
        }

        var step = dsl.insertInto(EN_PRODUCT)
                .set(EN_PRODUCT.ID, product.id());

        if (product.name() != null) {
            step.set(EN_PRODUCT.NAME, product.name());
        }
        if (product.price() != null) {
            step.set(EN_PRODUCT.PRICE, BigDecimal.valueOf(product.price()));
        }
        if (product.description() != null) {
            step.set(EN_PRODUCT.DESCRIPTION, product.description());
        }
        if (product.image() != null) {
            step.set(EN_PRODUCT.IMAGE, product.image());
        }

        return step.execute() > 0;
    }

    @Override
    public boolean update(Product newBean, Product oldBean, Product projection) {
        if (newBean == null) {
            throw new InvalidRequestException("update of Product without the new state");
        }

        if (newBean.id() == null) {
            throw new InvalidRequestException("update of Product without the key (id)");
        }

        if (projection == null) {
            projection = this.newProjection();
        }

        var dsl = dsl();
        var step = dsl.update(EN_PRODUCT).set(EN_PRODUCT.ID, newBean.id());

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, p -> p.name())) {
            step.set(EN_PRODUCT.NAME, newBean.name());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, p -> p.price())) {
            step.set(EN_PRODUCT.PRICE, newBean.price() != null ? BigDecimal.valueOf(newBean.price()) : null);
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, p -> p.description())) {
            step.set(EN_PRODUCT.DESCRIPTION, newBean.description());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, p -> p.image())) {
            step.set(EN_PRODUCT.IMAGE, newBean.image());
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_PRODUCT.ID.eq(newBean.id())).execute() > 0;
    }

    @Override
    public int delete(ProductCriteria criteria) {
        // The field always exists; what matters is whether it is set. Without this guard, empty criteria would
        // translate to noCondition() and the DELETE would wipe the whole table.
        if (criteria == null || !criteria.hasProductId()) {
            throw new InvalidRequestException("delete of Product without the key (id)");
        }

        return dsl()
                .deleteFrom(EN_PRODUCT)
                .where(applyConditions(EN_PRODUCT, criteria))
                .execute();
    }

    @Override
    public int count(ProductCriteria criteria) {
        return dsl().selectCount()
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
    public byte[] fetchImage(Long productId) {
        var prjBean = new Product()
                .withId(0L)
                .withImage(new byte[0]);

        var product = QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(productId)));
        if (product == null) {
            throw new BusinessException("Product not found: " + productId);
        }
        return product.image();
    }

    @Override
    public boolean updateImage(Long productId, byte[] image) {
        return dsl()
                .update(EN_PRODUCT)
                .set(EN_PRODUCT.IMAGE, image)
                .where(EN_PRODUCT.ID.eq(productId))
                .execute() > 0;
    }

    // :: Internal

    private Product projectionFrom(ProductCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            // Always include id
            if (prj.id() == null) {
                prj.withId(0L);
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
            this.ctx = new QueryContext(dsl());
        }

        /**
         * The conditions for the fields that are set.
         *
         * <p>
         * Walking the fields and assembling the {@code AND} live in {@link CriterionTranslator}; here is only what is
         * specific to the entity — the column of each field. Empty criteria result in {@code noCondition()}, not in a
         * false condition, which would turn "no filter" into "no results".
         * </p>
         */
        public Condition apply(ProductCriteria criteria) {
            if (criteria == null) {
                return DSL.noCondition();
            }
            return CriterionTranslator.and(Arrays.asList(
                    CriterionTranslator.translate(enProduct.ID, criteria.productId()),
                    CriterionTranslator.translate(enProduct.NAME, criteria.name()),
                    // The column is NUMERIC; the domain speaks Double. The conversion belongs to the field, not the
                    // operator, so it applies equally to eq, between and in.
                    CriterionTranslator.translate(enProduct.PRICE, criteria.price(), BigDecimal::valueOf),
                    CriterionTranslator.translate(enProduct.DESCRIPTION, criteria.description())));
        }
    }
}
