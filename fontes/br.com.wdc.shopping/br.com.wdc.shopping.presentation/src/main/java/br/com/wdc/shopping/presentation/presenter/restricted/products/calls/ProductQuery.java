package br.com.wdc.shopping.presentation.presenter.restricted.products.calls;

import br.com.wdc.shopping.presentation.ShoppingQuery;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

/** A product, with its description. */
public class ProductQuery extends ShoppingQuery<ProductInfo> {

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

        var product = productRepository().fetchById(productId, ProductInfo.projection());
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ProductInfo.create(product);
    }

}
