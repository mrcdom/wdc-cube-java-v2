package br.com.wdc.shopping.business.impl.sgbd.repository.purchaseitem;

import java.util.List;

import br.com.wdc.framework.commons.util.TransactionContext;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseRepository;
import br.com.wdc.shopping.business.shared.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;
import br.com.wdc.shopping.business.shared.repositories.PurchaseItemRepository;

public class PurchaseItemRepositoryImpl extends BaseRepository implements PurchaseItemRepository {

    @Override
    public boolean insert(PurchaseItem purchaseItem) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return InsertRowPurchaseItemCmd.run(tx.connection(), purchaseItem);
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public boolean insertOrUpdate(PurchaseItem purchaseItem) {
        try (var tx = TransactionContext.begin(dataSource())) {
            var modified = false;
            if (purchaseItem.id == null) {
                modified = InsertRowPurchaseItemCmd.run(tx.connection(), purchaseItem);
            } else {
                modified = UpdateRowPurchaseItemCmd.run(tx.connection(), purchaseItem);
            }
            return modified;
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public boolean update(PurchaseItem newPurchaseItem, PurchaseItem oldPurchaseItem) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return UpdateRowPurchaseItemCmd.run(tx.connection(), newPurchaseItem, oldPurchaseItem);
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }
    
    @Override
    public int delete(PurchaseItemCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return DeletePurchaseItemsCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public int count(PurchaseItemCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return CountPurchaseItemsCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public List<PurchaseItem> fetch(PurchaseItemCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchPurchaseItemsCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public PurchaseItem fetchById(Long purchaseId, PurchaseItem projection) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchPurchaseItemsCmd.byId(tx.connection(), purchaseId, projection);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

}
