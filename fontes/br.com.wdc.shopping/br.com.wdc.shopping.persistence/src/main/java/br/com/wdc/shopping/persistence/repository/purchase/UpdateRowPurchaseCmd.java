package br.com.wdc.shopping.persistence.repository.purchase;

import java.sql.Connection;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.schema.EnPurchase;
import br.com.wdc.shopping.persistence.schema.EnPurchase.Row;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.sql.SqlUtils;
import br.com.wdc.shopping.domain.model.Purchase;

public class UpdateRowPurchaseCmd extends BaseCommand {

    public static boolean run(Connection connection, Purchase bean) {
        if (bean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        return new UpdateRowPurchaseCmd().execute(connection, rowFromBean(bean)) > 0;
    }

    public static boolean run(Connection connection, Purchase newBean, Purchase oldBean) {
        if (newBean.id == null) {
            throw new AssertionError("Missing primary key in newUser");
        }

        if (oldBean.id == null) {
            throw new AssertionError("Missing primary key in oldUser");
        }

        if (!Objects.equals(newBean.id, oldBean.id)) {
            throw new AssertionError("New and old bean must have some key value");
        }

        var row = rowFromBean(oldBean);
        row.clearChanges();

        var hasChanges = false;

        var userId = newBean.user != null ? newBean.user.id : null;
        if (Objects.equals(row.userId(), userId)) {
            row.userId(userId);
            hasChanges = true;
        }

        if (Objects.equals(row.buyDate(), newBean.buyDate)) {
            row.buyDate(newBean.buyDate);
            hasChanges = true;
        }

        if (hasChanges) {
            return new UpdateRowPurchaseCmd().execute(connection, row) > 0;
        }

        return false;
    }

    public int execute(Connection connection, Row row) {
        var en = EnPurchase.INSTANCE;

        var sql = new SqlList();

        sql.ln(UPDATE, en.tableName(), SET);

        var comma = SqlUtils.comma();
        if (row.isUserIdChanged()) {
            sql.ln(comma.get(), en.userId, EQUAL, param("userId", row.userId()));
        }

        if (row.isBuyDateChanged()) {
            sql.ln(comma.get(), en.buyDate, EQUAL, param("buyDate", row.buyDate()));
        }

        if (paramsIsEmpty()) {
            return 0;
        }

        if (row.id() != null) {
            sql.ln(WHERE, en.id, EQUAL, param("id", row.id()));
        } else {
            throw new AssertionError("Missing primary key");
        }

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

    private static EnPurchase.Row rowFromBean(Purchase bean) {
        var row = new EnPurchase.Row();
        row.id(bean.id);
        if (bean.user != null) {
            row.userId(bean.user.id);
        }
        row.buyDate(bean.buyDate);
        return row;
    }

}
