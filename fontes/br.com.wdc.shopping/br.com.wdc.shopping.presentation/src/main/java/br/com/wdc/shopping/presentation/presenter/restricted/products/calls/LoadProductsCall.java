package br.com.wdc.shopping.presentation.presenter.restricted.products.calls;

import java.util.List;

import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.presentation.ShoppingCall;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

/** The products for a list: without their descriptions, which only the product page shows. */
public class LoadProductsCall extends ShoppingCall<List<ProductInfo>> {

    private int limit;

    public LoadProductsCall withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public List<ProductInfo> run() {
        var criteria = new ProductCriteria()
                .withProjection(ProductInfo.projection());

        criteria.projection().withDescription(null);

        return productRepository().fetch(criteria, 0, limit)
                .stream().map(ProductInfo::create).toList();
    }

}
