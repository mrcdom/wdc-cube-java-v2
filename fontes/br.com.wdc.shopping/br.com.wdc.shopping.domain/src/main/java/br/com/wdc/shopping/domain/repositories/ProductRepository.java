package br.com.wdc.shopping.domain.repositories;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public interface ProductRepository extends Repository<Product, ProductCriteria, Long> {

    AtomicReference<ProductRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Product newProjection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new Product();
        prj.id = pv.i64;
        prj.name = pv.str;
        prj.price = pv.f64;
        prj.description = pv.str;
        prj.image = pv.bin;
        return prj;
    }

    byte[] fetchImage(Long productId);

    boolean updateImage(Long productId, byte[] image);

}
