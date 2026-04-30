package br.com.wdc.shopping.domain.criteria;

import br.com.wdc.shopping.domain.model.Purchase;

public class PurchaseCriteria {

    // :: Projection

    private Purchase projection;

    public Purchase projection() {
        return projection;
    }

    public PurchaseCriteria withProjection(Purchase projection) {
        this.projection = projection;
        return this;
    }

    // :: Offset and limit

    private Integer offset;

    public Integer offset() {
        return offset;
    }

    public PurchaseCriteria withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    private Integer limit;

    public Integer limit() {
        return limit;
    }

    public PurchaseCriteria withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    // :: Criteria

    private Long purchaseId;

    public Long purchaseId() {
        return purchaseId;
    }

    public PurchaseCriteria withPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    private Long userId;

    public Long userId() {
        return userId;
    }

    public PurchaseCriteria withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public PurchaseCriteria withOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public enum OrderBy {
        ACENDING,
        DESCENDING
    }
}
