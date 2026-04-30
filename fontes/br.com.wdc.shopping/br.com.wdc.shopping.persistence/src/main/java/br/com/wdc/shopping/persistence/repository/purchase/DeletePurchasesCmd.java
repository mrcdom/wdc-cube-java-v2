package br.com.wdc.shopping.persistence.repository.purchase;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnPurchase;
import br.com.wdc.shopping.persistence.sql.SqlList;

public class DeletePurchasesCmd extends BaseCommand {

    public static int byId(Connection connection, Long purchaseId) {
        if (purchaseId == null) {
            throw new AssertionError("purchaseId is required");
        }
        return new DeletePurchasesCmd().execute(connection, new PurchaseCriteria().withPurchaseId(purchaseId));
    }

    public static int byCriteria(Connection connection, PurchaseCriteria criteria) {
        return new DeletePurchasesCmd().execute(connection, criteria);
    }

    // :: Action

    public int execute(Connection connection, PurchaseCriteria criteria) {
        var en = EnPurchase.INSTANCE;

        var sql = new SqlList();
        sql.ln(DELETE);
        sql.ln(FROM, en.tableName());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyPurshaseCriteria(this);
        applier.criteria = criteria;
        applier.root = en;
        applier.apply(sql);

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
