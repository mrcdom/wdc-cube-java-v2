package br.com.wdc.shopping.persistence.sgbd.repository.purchaseitem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnPurchaseItem.Row;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.model.PurchaseItem;

public class InsertRowPurchaseItemCmd extends BaseCommand {

    public static boolean run(Connection connection, PurchaseItem bean) {
        var row = new EnPurchaseItem.Row();
        row.id(bean.id);

        if (bean.purchase != null && bean.purchase.id != null) {
            row.purchaseId(bean.purchase.id);
        }

        if (bean.product != null && bean.product.id != null) {
            row.productId(bean.product.id);
        }

        if (bean.amount != null) {
            row.amount(bean.amount);
        }

        if (bean.price != null) {
            row.price(BigDecimal.valueOf(bean.price));
        }

        var inserted = new InsertRowPurchaseItemCmd().execute(connection, row) > 0;
        bean.id = row.id();
        return inserted;
    }

    public int execute(Connection connection, Row row) {
        var en = EnPurchaseItem.INSTANCE;

        this.checkContraints(row);

        if (row.id() == null) {
            row.id(en.nextSeqPurchaseItem(connection));
        }

        var sql = new SqlList();
        var places = new ArrayList<String>();

        sql.ln(INSERT_INTO, en.tableName(), '(');

        sql.ln(' ', en.id);
        places.add(param("id", row.id()));

        if (row.isPurchaseIdChanged()) {
            sql.ln(',', en.purchaseId);
            places.add(param("purchaseId", row.purchaseId()));
        }

        if (row.isProductIdChanged()) {
            sql.ln(',', en.productId);
            places.add(param("productId", row.productId()));
        }

        if (row.isAmountChanged()) {
            sql.ln(',', en.amount);
            places.add(param("amount", row.amount()));
        }

        if (row.isPriceChanged()) {
            sql.ln(',', en.price);
            places.add(param("price", row.price()));
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

    private void checkContraints(Row row) throws AssertionError {
        if (row.purchaseId() == null) {
            throw new AssertionError("purchaseId is required");
        }

        if (row.productId() == null) {
            throw new AssertionError("productId is required");
        }

        if (row.amount() == null) {
            throw new AssertionError("amount is required");
        }

        if (row.price() == null) {
            throw new AssertionError("price is required");
        }

        if (row.amount() <= 0) {
            throw new AssertionError("amount must be grether than ZERO");
        }
    }

}
