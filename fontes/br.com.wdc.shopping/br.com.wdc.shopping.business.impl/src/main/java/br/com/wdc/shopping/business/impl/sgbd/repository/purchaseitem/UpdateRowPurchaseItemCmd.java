package br.com.wdc.shopping.business.impl.sgbd.repository.purchaseitem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.impl.sgbd.utils.SqlUtils;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;

public class UpdateRowPurchaseItemCmd extends BaseCommand {

    public static boolean run(Connection connection, PurchaseItem bean) {
        if (bean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        return new UpdateRowPurchaseItemCmd().execute(connection, rowFromBean(bean)) > 0;
    }

    public static boolean run(Connection connection, PurchaseItem newBean, PurchaseItem oldBean) {
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

        var purchaseId = newBean.purchase != null ? newBean.purchase.id : null;
        if (Objects.equals(row.purchaseId(), purchaseId)) {
            row.purchaseId(purchaseId);
            hasChanges = true;
        }

        var productId = newBean.product != null ? newBean.product.id : null;
        if (Objects.equals(row.productId(), productId)) {
            row.productId(productId);
            hasChanges = true;
        }

        if (Objects.equals(row.amount(), newBean.amount)) {
            row.amount(newBean.amount);
            hasChanges = true;
        }

        var newPrice = newBean.price != null ? BigDecimal.valueOf(newBean.price) : null;
        if (Objects.equals(row.price(), newPrice)) {
            row.price(newPrice);
            hasChanges = true;
        }

        if (hasChanges) {
            return new UpdateRowPurchaseItemCmd().execute(connection, row) > 0;
        }

        return false;
    }

    public int execute(Connection connection, EnPurchaseItem.Row row) {
        var en = EnPurchaseItem.INSTANCE;

        var sql = new SqlList();

        sql.ln(UPDATE, en.tableName(), SET);

        var comma = SqlUtils.comma();
        if (row.isPurchaseIdChanged()) {
            sql.ln(comma.get(), en.purchaseId, EQUAL, param("purchaseId", row.purchaseId()));
        }

        if (row.isProductIdChanged()) {
            sql.ln(comma.get(), en.productId, EQUAL, param("productId", row.productId()));
        }

        if (row.isAmountChanged()) {
            sql.ln(comma.get(), en.amount, EQUAL, param("amount", row.amount()));
        }

        if (row.isPriceChanged()) {
            sql.ln(comma.get(), en.price, EQUAL, param("price", row.price()));
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

    private static EnPurchaseItem.Row rowFromBean(PurchaseItem bean) {
        var row = new EnPurchaseItem.Row();
        row.id(bean.id);

        if (bean.purchase != null) {
            row.purchaseId(bean.purchase.id);
        }

        if (bean.product != null) {
            row.productId(bean.product.id);
        }

        row.amount(bean.amount);

        if (bean.price != null) {
            row.price(BigDecimal.valueOf(bean.price));
        }

        return row;
    }

}
