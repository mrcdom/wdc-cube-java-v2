package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.model.Page;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
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
        return repo.fetch(criteria.withProjection(PurchaseInfo.projectionWithItens()))
                .stream().map(PurchaseInfo::create).toList();
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
                .withProjection(PurchaseInfo.projectionWithItens())
                .withOrderBy(OrderBy.DESCENDING),
                offset != null ? offset : 0,
                limit != null ? limit : 0)
                .stream().map(PurchaseInfo::create).toList();
    }

    public Page<PurchaseInfo> fetchPageOfUser(Long userId, int page, int pageSize) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }

        var result = repo.fetchPage(new PurchaseCriteria()
                .withUserId(userId)
                .withProjection(PurchaseInfo.projectionWithItens())
                .withOrderBy(OrderBy.DESCENDING), page, pageSize);

        var items = result.items().stream().map(PurchaseInfo::create).toList();
        return new Page<>(items, result.page(), result.totalPages(), result.totalItems());
    }
}