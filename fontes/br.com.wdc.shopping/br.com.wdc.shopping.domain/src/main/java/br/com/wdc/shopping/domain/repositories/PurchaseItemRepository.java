package br.com.wdc.shopping.domain.repositories;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;

public interface PurchaseItemRepository {
    
    AtomicReference<PurchaseItemRepository> BEAN = new AtomicReference<>();
    
    boolean insert(PurchaseItem purchaseItem);
    
    boolean insertOrUpdate(PurchaseItem purchaseItem);

    boolean update(PurchaseItem newPurchaseItem, PurchaseItem oldPurchaseItem);
    
    int delete(PurchaseItemCriteria criteria);
    
    int count(PurchaseItemCriteria criteria);

    List<PurchaseItem> fetch(PurchaseItemCriteria criteria);

    PurchaseItem fetchById(Long purchaseId, PurchaseItem projection);

}
