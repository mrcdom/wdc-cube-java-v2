package br.com.wdc.shopping.persistence.repository.purchase;

import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.schema.EnPurchase;
import br.com.wdc.shopping.persistence.repository.BaseApplyCriteria;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;

public class ApplyPurshaseCriteria extends BaseApplyCriteria {

    EnPurchase root;
    PurchaseCriteria criteria;

    protected ApplyPurshaseCriteria(BaseCommand cmd) {
        super(cmd);
    }

    @Override
    public void apply(SqlList sql) {
        if (criteria.purchaseId() != null) {
            sql.ln(AND, root.id, EQUAL, param("purchaseId", criteria.purchaseId()));
        }

        if (criteria.userId() != null) {
            sql.ln(AND, root.userId, EQUAL, param("userId", criteria.userId()));
        }
    }

}
