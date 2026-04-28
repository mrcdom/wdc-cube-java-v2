package br.com.wdc.shopping.business.shared.repositories;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.business.shared.criteria.ProductCriteria;
import br.com.wdc.shopping.business.shared.model.Product;

public interface ProductRepository {

    AtomicReference<ProductRepository> BEAN = new AtomicReference<>();

    boolean insert(Product product);

    boolean update(Product newProduct, Product oldProduct);

    boolean insertOrUpdate(Product product);

    int delete(ProductCriteria criteria);

    int count(ProductCriteria criteria);

    List<Product> fetch(ProductCriteria criteria);

    Product fetchById(Long productId, Product projection);

    byte[] fetchImage(Long productId);

}
