package br.com.wdc.store.presentation.presenter.restricted.products.calls;

import br.com.wdc.framework.domain.resource.ResourceQuery;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.presentation.exception.ProductNotFoundException;
import br.com.wdc.store.presentation.exception.WrongParametersException;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;

/** A product, with its description. */
public class ProductQuery extends ResourceQuery<ProductInfo> {

    private Long productId;

    public ProductQuery withProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    @Override
    public ProductInfo run() {
        if (productId == null) {
            throw new WrongParametersException();
        }

        var product = ProductRepository.BEAN.get().fetchById(productId, ProductInfo.projection());
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductInfo.create(product);
    }

}
