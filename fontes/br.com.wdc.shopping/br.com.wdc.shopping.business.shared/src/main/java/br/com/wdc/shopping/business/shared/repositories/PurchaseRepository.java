package br.com.wdc.shopping.business.shared.repositories;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.business.shared.criteria.PurchaseCriteria;
import br.com.wdc.shopping.business.shared.model.Purchase;

public interface PurchaseRepository {

    AtomicReference<PurchaseRepository> BEAN = new AtomicReference<>();
    
    boolean insert(Purchase purchase);
    
    boolean insertOrUpdate(Purchase purchase);

    boolean update(Purchase newPurchase, Purchase oldPurchase);

    int count(PurchaseCriteria criteria);

    List<Purchase> fetch(PurchaseCriteria criteria);

    Purchase fetchById(Long purchaseId, Purchase projection);

}
