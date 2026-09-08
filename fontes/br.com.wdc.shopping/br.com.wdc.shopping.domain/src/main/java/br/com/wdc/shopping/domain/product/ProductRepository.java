package br.com.wdc.shopping.domain.product;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;

public interface ProductRepository extends Repository<Product, ProductCriteria, Long> {

    AtomicReference<ProductRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Product newProjection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new Product()
                .withId(pv.i64)
                .withName(pv.str)
                .withPrice(pv.f64)
                .withDescription(pv.str)
                .withImage(null);
        return prj;
    }

    byte[] fetchImage(Long productId);

    boolean updateImage(Long productId, byte[] image);

}
