package br.com.wdc.shopping.presentation.presenter.restricted.home;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria.OrderBy;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;

public enum HomeService {
    BEAN;

    public List<PurchaseInfo> loadPurchases(PurchaseCriteria criteria) {
        return PurchaseRepository.BEAN.get()
                .fetch(criteria.withProjection(PurchaseInfo.projectionWithItens()))
                .stream().map(PurchaseInfo::create).toList();
    }

    public int countPurchasesOfUser(Long userId) {
        return PurchaseRepository.BEAN.get().count(new PurchaseCriteria().withUserId(userId));
    }

    public List<PurchaseInfo> loadPurchasesOfUser(Long userId) {
        return this.loadPurchasesOfUser(userId, null, null);
    }

    public List<PurchaseInfo> loadPurchasesOfUser(Long userId, Integer offset, Integer limit) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }

        return PurchaseRepository.BEAN.get().fetch(new PurchaseCriteria()
                .withUserId(userId)
                .withProjection(PurchaseInfo.projectionWithItens())
                .withOrderBy(OrderBy.DESCENDING)
                .withOffset(offset)
                .withLimit(limit))
                .stream().map(PurchaseInfo::create).toList();
    }
}