package br.com.wdc.shopping.persistence.sgbd.repository.purchase;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseApplyCriteria;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
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
