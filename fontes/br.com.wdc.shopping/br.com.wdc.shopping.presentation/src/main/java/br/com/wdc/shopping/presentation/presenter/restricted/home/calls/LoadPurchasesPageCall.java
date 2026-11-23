package br.com.wdc.shopping.presentation.presenter.restricted.home.calls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.domain.pagination.Page;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.presentation.ShoppingCall;
import br.com.wdc.shopping.presentation.exception.WrongParametersException;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;

/** A page of a user's purchases, the most recent first. */
public class LoadPurchasesPageCall extends ShoppingCall<Page<PurchaseInfo>> {

    private Long userId;
    private int page;
    private int pageSize;

    public LoadPurchasesPageCall withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public LoadPurchasesPageCall withPage(int page) {
        this.page = page;
        return this;
    }

    public LoadPurchasesPageCall withPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public Page<PurchaseInfo> run() {
        if (userId == null) {
            throw new WrongParametersException();
        }

        var result = purchaseRepository().fetchPage(new PurchaseCriteria()
                .withUserId(userId)
                .withProjection(purchaseProjection())
                .withOrderBy(OrderBy.MOST_RECENT_PURCHASE_FIRST), page, pageSize);
        var items = result.items().stream().map(LoadPurchasesPageCall::purchaseInfoOf).toList();
        return new Page<>(items, result.page(), result.totalPages(), result.totalItems());
    }

    // :: Internal

    private static Purchase purchaseProjection() {
        var pv = ProjectionValues.INSTANCE;
        var prdPrj = new Product()
                .withName(pv.str);
        var itemPrj = new PurchaseItem()
                .withPrice(pv.f64)
                .withAmount(pv.i32)
                .withProduct(prdPrj);
        return new Purchase()
                .withId(pv.i64)
                .withBuyDate(pv.offsetDateTime)
                .withItems(Collections.singletonList(itemPrj));
    }

    private static PurchaseInfo purchaseInfoOf(Purchase src) {
        if (src == null) {
            return null;
        }

        var pv = ProjectionValues.INSTANCE;
        var tgt = new PurchaseInfo();
        tgt.id = Optional.ofNullable(src.id()).orElse(-1L);
        var buyDate = CoerceUtils.asDate(src.buyDate());
        tgt.date = Optional.ofNullable(buyDate).orElse(pv.date).getTime();
        tgt.items = new ArrayList<>();
        var total = 0.0;
        for (var item : Optional.ofNullable(src.items()).orElse(Collections.emptyList())) {
            var price = Optional.ofNullable(item.price()).orElse(0.0);
            var amount = Optional.ofNullable(item.amount()).orElse(0);
            total += price * amount;
            if (item.product() != null && StringUtils.isNotBlank(item.product().name())) {
                tgt.items.add(item.product().name());
            }
        }
        tgt.total = total;
        return tgt;
    }

}
