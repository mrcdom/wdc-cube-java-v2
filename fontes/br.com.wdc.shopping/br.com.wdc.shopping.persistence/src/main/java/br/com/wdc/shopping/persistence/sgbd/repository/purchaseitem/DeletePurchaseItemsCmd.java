package br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.tables.EnPurchaseItem;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;

public class DeletePurchaseItemsCmd extends BaseCommand {

    public static int byId(Connection connection, Long purchaseItemId) {
        if (purchaseItemId == null) {
            throw new AssertionError("purchaseItemId is required");
        }
        return new DeletePurchaseItemsCmd().execute(connection, new PurchaseItemCriteria()
                .withPurchaseItemId(purchaseItemId));
    }

    public static int byCriteria(Connection connection, PurchaseItemCriteria criteria) {
        return new DeletePurchaseItemsCmd().execute(connection, criteria);
    }

    public int execute(Connection connection, PurchaseItemCriteria criteria) {
        if (criteria == null) {
            criteria = new PurchaseItemCriteria();
        }

        var en = new EnPurchaseItem("pi");

        var sql = new SqlList();
        sql.ln(DELETE);
        sql.ln(FROM, en.tableRef());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyPurshaseItemCriteria(this);
        applier.root = en;
        applier.criteria = criteria;
        applier.apply(sql);

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
