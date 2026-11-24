package br.com.wdc.store.domain.product;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;

public interface ProductRepository extends Repository<Product, ProductCriteria, Long> {

    AtomicReference<ProductRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Product newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new Product()
                .withId(pv.i64)
                .withName(pv.str)
                .withPrice(pv.f64)
                .withDescription(pv.str)
                .withImage(null);
    }

    /**
     * Lookup by key: a {@code ProductCriteria} with equality on the primary key, resolved by {@code fetch}.
     *
     * @param projection {@code null} projects {@link #newProjection()} — all the entity's shallow fields.
     * @return the entity, or {@code null} if there is no row with that key.
     */
    @Override
    default Product fetchById(Long productId, Product projection) {
        if (productId == null) {
            return null;
        }

        var found = fetch(new ProductCriteria()
                .withProductId(productId)
                .withProjection(projection != null ? projection : newProjection()), 0, 1);

        return found.isEmpty() ? null : found.get(0);
    }

    byte[] fetchImage(Long productId);

    boolean updateImage(Long productId, byte[] image);

}
