package br.com.wdc.shopping.presentation.presenter.restricted.products;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public enum ProductService {
    BEAN;

    public ProductInfo loadProductById(Long productId) {
        var shoppingService = ProductRepository.BEAN.get();

        if (productId == null) {
            throw new WrongParametersException();
        }

        var product = shoppingService.fetchById(productId, ProductInfo.projection());
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductInfo.create(product);
    }

    public List<ProductInfo> loadProductsWithoutDescription(int limit) {
        var shoppingService = ProductRepository.BEAN.get();

        var criteria = new ProductCriteria()
                .withProjection(ProductInfo.projection())
                .withLimit(limit); // safe limit (paging implementation is required)

        // This long description will not be shown
        criteria.projection().description = null;

        return shoppingService.fetch(criteria)
                .stream().map(ProductInfo::create).toList();
    }
}