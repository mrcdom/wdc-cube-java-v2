package br.com.wdc.shopping.persistence.repository.purchase;

import java.util.List;

import br.com.wdc.framework.commons.util.TransactionContext;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.persistence.repository.BaseRepository;

public class PurchaseRepositoryImpl extends BaseRepository implements PurchaseRepository {

    @Override
    public boolean insert(Purchase purchase) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return InsertRowPurchaseCmd.runWithItems(tx.connection(), purchase);
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public boolean insertOrUpdate(Purchase purchase) {
        try (var tx = TransactionContext.begin(dataSource())) {
            var modified = false;
            if (purchase.id == null) {
                modified = InsertRowPurchaseCmd.runWithItems(tx.connection(), purchase);
            } else {
                modified = UpdateRowPurchaseCmd.run(tx.connection(), purchase);
            }

            return modified;
        } catch (Exception caught) {
            throw writeException(caught);
        }
    }

    @Override
    public boolean update(Purchase newPurchase, Purchase oldPurchase) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public int delete(PurchaseCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return DeletePurchasesCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public int count(PurchaseCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return CountPurchasesCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public List<Purchase> fetch(PurchaseCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchPurchaseCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public Purchase fetchById(Long purchaseId, Purchase projection) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchPurchaseCmd.byId(tx.connection(), purchaseId, projection);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

}
