package br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseApplyCriteria;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;

public class ApplyPurshaseItemCriteria extends BaseApplyCriteria {

    EnPurchaseItem root;
    PurchaseItemCriteria criteria;

    protected ApplyPurshaseItemCriteria(BaseCommand cmd) {
        super(cmd);
    }

    public void apply(SqlList sql) {
        if (criteria.purchaseItemId() != null) {
            sql.ln(AND, root.id, EQUAL, param("purchaseItemId", criteria.purchaseItemId()));
        }

        if (criteria.purchaseId() != null) {
            sql.ln(AND, root.purchaseId, EQUAL, param("purchaseId", criteria.purchaseId()));
        }

        if (criteria.productId() != null) {
            sql.ln(AND, root.productId, EQUAL, param("productId", criteria.productId()));
        }

        if (criteria.userId() != null) {
            sql.ln(AND, EXISTS(buildPurchaseCriteria()));
        }
    }

    // :: Internal

    private SqlList buildPurchaseCriteria() {
        EnPurchase p = new EnPurchase("P");

        var sql = new SqlList();
        sql.ln(SELECT, 1);
        sql.ln(FROM, p.tableRef());
        sql.ln(WHERE, p.id, EQUAL, root.purchaseId);

        if (criteria.userId() != null) {
            sql.ln(AND, p.userId, EQUAL, param("userId", criteria.userId()));
        }

        return sql;
    }

}
