package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.pagination.Page;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;

public class PurchasesPanelService {

    private final PurchaseRepository repo;

    public PurchasesPanelService(ShoppingApplication app) {
        this.repo = app.getPurchaseRepository();
    }

    public PurchasesPanelService(PurchaseRepository repo) {
        this.repo = repo;
    }

    public List<PurchaseInfo> loadPurchases(PurchaseCriteria criteria) {
        return repo.fetch(criteria.withProjection(purchaseProjection()))
                .stream().map(this::purchaseInfoCreate).toList();
    }

    public int countPurchasesOfUser(Long userId) {
        return repo.count(new PurchaseCriteria().withUserId(userId));
    }

    public List<PurchaseInfo> loadPurchasesOfUser(Long userId) {
        return this.loadPurchasesOfUser(userId, null, null);
    }

    public List<PurchaseInfo> loadPurchasesOfUser(Long userId, Integer offset, Integer limit) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }

        return repo.fetch(new PurchaseCriteria()
                .withUserId(userId)
                .withProjection(purchaseProjection())
                .withOrderBy(OrderBy.DESCENDING),
                offset != null ? offset : 0,
                limit != null ? limit : 0)
                .stream().map(this::purchaseInfoCreate).toList();
    }

    public Page<PurchaseInfo> fetchPageOfUser(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }

        var result = repo.fetchPage(new PurchaseCriteria()
                .withUserId(userId)
                .withProjection(purchaseProjection())
                .withOrderBy(OrderBy.DESCENDING), page, pageSize);

        var items = result.items().stream().map(this::purchaseInfoCreate).toList();
        return new Page<>(items, result.page(), result.totalPages(), result.totalItems());
    }

    // :: Internal

    private Purchase purchaseProjection() {
        var pv = ProjectionValues.INSTANCE;

        var prdPrj = new Product();
        prdPrj.name = pv.str;

        var itemPrj = new PurchaseItem();
        itemPrj.price = pv.f64;
        itemPrj.amount = pv.i32;
        itemPrj.product = prdPrj;

        var prj = new Purchase();
        prj.id = pv.i64;
        prj.buyDate = pv.offsetDateTime;
        prj.items = Collections.singletonList(itemPrj);

        return prj;
    }

    private PurchaseInfo purchaseInfoCreate(Purchase src) {
        if (src == null) {
            return null;
        }

        var pv = ProjectionValues.INSTANCE;

        var tgt = new PurchaseInfo();
        tgt.id = Optional.ofNullable(src.id).orElse(-1L);

        var buyDate = CoerceUtils.asDate(src.buyDate);
        tgt.date = Optional.ofNullable(buyDate).orElse(pv.date).getTime();
        tgt.items = new ArrayList<>();

        var total = 0.0;
        for (var item : Optional.ofNullable(src.items).orElse(Collections.emptyList())) {
            var price = Optional.ofNullable(item.price).orElse(0.0);
            var amount = Optional.ofNullable(item.amount).orElse(0);
            total += price * amount;

            if (item.product != null && StringUtils.isNotBlank(item.product.name)) {
                tgt.items.add(item.product.name);
            }
        }

        tgt.total = total;

        return tgt;
    }
}