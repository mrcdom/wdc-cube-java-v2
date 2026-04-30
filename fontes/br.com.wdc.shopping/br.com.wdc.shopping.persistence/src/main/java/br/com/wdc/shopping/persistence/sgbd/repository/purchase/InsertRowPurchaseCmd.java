package br.com.wdc.shopping.persistence.sgbd.repository.purchase;

import java.sql.Connection;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem.InsertRowPurchaseItemCmd;
import br.com.wdc.shopping.persistence.sgbd.tables.EnPurchase;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.model.Purchase;

public class InsertRowPurchaseCmd extends BaseCommand {

    public static boolean run(Connection connection, Purchase bean) {
        var row = new EnPurchase.Row();
        row.id(bean.id);

        if (bean.user != null && bean.user.id != null) {
            row.userId(bean.user.id);
        }

        if (bean.buyDate != null) {
            row.buyDate(bean.buyDate);
        }

        var inserted = new InsertRowPurchaseCmd().execute(connection, row) > 0;
        bean.id = row.id();
        return inserted;
    }

    public static boolean runWithItems(Connection connection, Purchase purchase) {
        if (!InsertRowPurchaseCmd.run(connection, purchase)) {
            return false;
        }

        if (purchase.items != null && !purchase.items.isEmpty()) {
            for (var item : purchase.items) {
                item.purchase = purchase;
                InsertRowPurchaseItemCmd.run(connection, item);
            }
        }

        return true;
    }

    public int execute(Connection connection, EnPurchase.Row row) {
        var en = EnPurchase.INSTANCE;

        checkContraints(row);

        if (row.id() == null) {
            row.id(en.nextSeqPurchase(connection));
        }

        var sql = new SqlList();
        var places = new ArrayList<String>();

        sql.ln(INSERT_INTO, en.tableName(), '(');

        sql.ln(' ', en.id);
        places.add(param("id", row.id()));

        if (row.isUserIdChanged()) {
            sql.ln(',', en.userId);
            places.add(param("userId", row.userId()));
        }

        if (row.isBuyDateChanged()) {
            sql.ln(',', en.buyDate);
            places.add(param("buyDate", row.buyDate()));
        }

        sql.add(")");

        sql.add(VALUES);
        sql.add("(" + StringUtils.join(places, ",") + ")");

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

    private void checkContraints(EnPurchase.Row row) throws AssertionError {
        if (row.userId() == null) {
            throw new AssertionError("userId is required");
        }

        if (row.buyDate() == null) {
            throw new AssertionError("buyDate is required");
        }
    }

}
