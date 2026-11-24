package br.com.wdc.store.presentation.presenter.restricted.products.calls;

import java.util.List;

import br.com.wdc.store.domain.product.ProductCriteria;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.presentation.StoreQuery;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;

/** The products for a list: without their descriptions, which only the product page shows. */
public class ProductListQuery extends StoreQuery<List<ProductInfo>> {

    private int limit;

    public ProductListQuery withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public List<ProductInfo> run() {
        var criteria = new ProductCriteria()
                .withProjection(ProductInfo.projection());

        criteria.projection()
                .withDescription(null);

        return ProductRepository.BEAN.get().fetch(criteria, 0, limit)
                .stream().map(ProductInfo::create).toList();
    }

}
