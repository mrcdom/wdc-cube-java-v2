package br.com.wdc.shopping.business.shared.criteria;

import br.com.wdc.shopping.business.shared.model.PurchaseItem;

public class PurchaseItemCriteria {

    // :: Projection

    private PurchaseItem projection;

    public PurchaseItem projection() {
        return projection;
    }

    public PurchaseItemCriteria withProjection(PurchaseItem projection) {
        this.projection = projection;
        return this;
    }

    // :: Limit and Offset

    private Integer offset;

    public Integer offset() {
        return offset;
    }

    public PurchaseItemCriteria withOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    private Integer limit;

    public Integer limit() {
        return limit;
    }

    public PurchaseItemCriteria withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    // :: Criteria

    private Long purchaseItemId;

    public Long purchaseItemId() {
        return purchaseItemId;
    }

    public PurchaseItemCriteria withPurchaseItemId(Long purchaseItemId) {
        this.purchaseItemId = purchaseItemId;
        return this;
    }

    private Long purchaseId;

    public Long purchaseId() {
        return purchaseId;
    }

    public PurchaseItemCriteria withPurchaseId(Long purchaseId) {
        this.purchaseId = purchaseId;
        return this;
    }

    private Long productId;

    public Long productId() {
        return productId;
    }

    public PurchaseItemCriteria withProductId(Long productId) {
        this.productId = productId;
        return this;
    }

    private Long userId;

    public Long userId() {
        return userId;
    }

    public PurchaseItemCriteria withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public PurchaseItemCriteria withOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public enum OrderBy {
        ACENDING,
        DESCENDING
    }

}
