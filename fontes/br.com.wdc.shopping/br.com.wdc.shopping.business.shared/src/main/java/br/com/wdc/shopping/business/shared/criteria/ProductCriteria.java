package br.com.wdc.shopping.business.shared.criteria;

import br.com.wdc.shopping.business.shared.model.Product;

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

    // :: Offset and limit

    private Integer offset;

    public Integer offset() {
        return offset;
    }

    public ProductCriteria withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    private Integer limit;

    public Integer limit() {
        return limit;
    }

    public ProductCriteria withLimit(Integer limit) {
        this.limit = limit;
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
        ACENDING,
        DESCENDING
    }

}
