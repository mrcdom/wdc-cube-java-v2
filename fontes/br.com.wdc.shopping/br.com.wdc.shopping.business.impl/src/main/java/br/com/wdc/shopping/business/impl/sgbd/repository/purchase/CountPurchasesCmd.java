package br.com.wdc.shopping.business.impl.sgbd.repository.purchase;

import java.sql.Connection;
import java.util.ArrayList;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.shared.criteria.PurchaseCriteria;

public class CountPurchasesCmd extends BaseCommand {

    public static int byCriteria(Connection connection, PurchaseCriteria criteria) {
        return new CountPurchasesCmd().execute(connection, criteria);
    }

    // :: Action

    public int execute(Connection connection, PurchaseCriteria criteria) {
        this.paramsList = new ArrayList<>();

        var pi = new EnPurchaseItem("PI");
        var b = new EnPurchase("B");

        var sql = new SqlList();
        sql.ln(SELECT, COUNT(DISTINCT, b.id));
        sql.ln(FROM, pi.tableRef());
        sql.ln(JOIN, b.tableRef(), ON, pi.purchaseId, EQUAL, b.id);

        sql.ln(WHERE_TRUE);

        var applier = new ApplyPurshaseCriteria(this);
        applier.criteria = criteria;
        applier.root = b;
        applier.apply(sql);

        // Read content
        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);
            return query.mapTo(Integer.class).one();
        }
    }

}
