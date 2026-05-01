package br.com.wdc.shopping.presentation.presenter.restricted.products;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class ProductService {

    private final ProductRepository repo;

    public ProductService(ShoppingApplication app) {
        this.repo = app.getProductRepository();
    }

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    public ProductInfo loadProductById(Long productId) {
        if (productId == null) {
            throw new WrongParametersException();
        }

        var product = repo.fetchById(productId, ProductInfo.projection());
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductInfo.create(product);
    }

    public List<ProductInfo> loadProductsWithoutDescription(int limit) {
        var criteria = new ProductCriteria()
                .withProjection(ProductInfo.projection())
                .withLimit(limit);

        criteria.projection().description = null;

        return repo.fetch(criteria)
                .stream().map(ProductInfo::create).toList();
    }
}