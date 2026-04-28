package br.com.wdc.shopping.business.impl.sgbd.repository.purchase;

import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseApplyCriteria;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.shared.criteria.PurchaseCriteria;

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
