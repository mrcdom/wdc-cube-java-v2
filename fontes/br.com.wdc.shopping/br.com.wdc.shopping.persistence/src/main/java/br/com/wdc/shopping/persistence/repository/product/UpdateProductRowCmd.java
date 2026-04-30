package br.com.wdc.shopping.persistence.repository.product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnProduct;
import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.sql.SqlUtils;

public class UpdateProductRowCmd extends BaseCommand {

    public static boolean run(Connection connection, Product product) {
        if (product.id == null) {
            throw new AssertionError("Missing primary key");
        }

        return new UpdateProductRowCmd().execute(connection, rowFromBean(product)) > 0;
    }

    public static boolean run(Connection connection, Product newBean, Product oldBean) {
        if (newBean.id == null) {
            throw new AssertionError("Missing primary key in newProd");
        }

        if (oldBean.id == null) {
            throw new AssertionError("Missing primary key in oldProd");
        }

        if (!Objects.equals(newBean.id, oldBean.id)) {
            throw new AssertionError("New and old bean must have some key value");
        }

        var row = rowFromBean(oldBean);

        var hasChanges = false;

        if (!Objects.equals(row.name(), newBean.name)) {
            row.name(newBean.name);
            hasChanges = true;
        }

        var newPrice = CoerceUtils.asBigDecimal(newBean.price);
        if (!Objects.equals(row.price(), newPrice)) {
            row.price(newPrice);
            hasChanges = true;
        }

        if (!Objects.equals(row.description(), newBean.description)) {
            row.description(newBean.description);
            hasChanges = true;
        }

        if (!Arrays.equals(row.image(), newBean.image)) {
            row.image(newBean.image);
            hasChanges = true;
        }

        if (hasChanges) {
            return new UpdateProductRowCmd().execute(connection, row) > 0;
        }

        return false;
    }

    public int execute(Connection connection, EnProduct.Row row) {
        var en = EnProduct.INSTANCE;

        var sql = new SqlList();

        sql.ln(UPDATE, en.tableName(), SET);

        var comma = SqlUtils.comma();
        if (row.isNameChanged()) {
            sql.ln(comma.get(), en.name, EQUAL, param("name", row.name()));
        }

        if (row.isPriceChanged()) {
            sql.ln(comma.get(), en.price, EQUAL, param("price", row.price()));
        }

        if (row.isDescriptionChanged()) {
            sql.ln(comma.get(), en.description, EQUAL, param("description", row.description()));
        }

        if (row.isImageChanged()) {
            sql.ln(comma.get(), en.image, EQUAL, param("image", row.image()));
        }

        if (this.paramsIsEmpty()) {
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

    private static EnProduct.Row rowFromBean(Product bean) {
        var row = new EnProduct.Row();
        row.id(bean.id);
        row.name(bean.name);
        row.description(bean.description);
        row.image(bean.image);
        row.price(BigDecimal.valueOf(bean.price));
        return row;
    }

}
