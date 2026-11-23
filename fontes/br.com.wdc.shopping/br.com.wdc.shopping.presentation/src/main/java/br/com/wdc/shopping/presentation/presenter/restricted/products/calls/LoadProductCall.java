package br.com.wdc.shopping.presentation.presenter.restricted.products.calls;

import br.com.wdc.shopping.presentation.ShoppingCall;
import br.com.wdc.shopping.presentation.exception.ProductNotFoundException;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

/** A product, with its description. */
public class LoadProductCall extends ShoppingCall<ProductInfo> {

    private Long productId;

    public LoadProductCall withProductId(Long productId) {
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
