package br.com.wdc.shopping.domain.criteria;

import br.com.wdc.shopping.domain.model.Product;

public class ProductCriteria {

    // :: Projection

    private Product projection;

    public Product projection() {
        return projection;
    }

    public ProductCriteria withProjection(Product projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private Long productId;

    public Long productId() {
        return productId;
    }

    public ProductCriteria withProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public ProductCriteria withOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }

}
