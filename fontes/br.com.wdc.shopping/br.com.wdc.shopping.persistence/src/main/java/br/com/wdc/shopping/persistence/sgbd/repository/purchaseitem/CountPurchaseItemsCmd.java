package br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.tables.EnPurchaseItem;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;

public class CountPurchaseItemsCmd extends BaseCommand {

    public static int byCriteria(Connection connection, PurchaseItemCriteria criteria) {
        return new CountPurchaseItemsCmd().execute(connection, criteria);
    }

    // :: Action

    protected int execute(Connection connection, PurchaseItemCriteria criteria) {
        var en = EnPurchaseItem.INSTANCE;

        var sql = new SqlList();
        sql.ln(SELECT, COUNT("*"));
        sql.ln(FROM, en.tableRef());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyPurshaseItemCriteria(this);
        applier.criteria = criteria;
        applier.root = en;
        applier.apply(sql);

        // Read content
        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);
            return query.mapTo(Integer.class).one();
        }
    }

}
