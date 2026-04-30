package br.com.wdc.shopping.persistence.repository.product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnProduct;
import br.com.wdc.shopping.persistence.sql.SqlList;

public class InsertProductRowCmd extends BaseCommand {

    public static boolean run(Connection connection, Product bean) {
        var row = new EnProduct.Row();
        row.id(bean.id);

        if (bean.name != null) {
            row.name(bean.name);
        }

        if (bean.description != null) {
            row.description(bean.description);
        }

        if (bean.image != null) {
            row.image(bean.image);
        }

        if (bean.price != null) {
            row.price(BigDecimal.valueOf(bean.price));
        }

        var inserted = new InsertProductRowCmd().execute(connection, row) > 0;
        bean.id = row.id();
        return inserted;
    }

    public int execute(Connection connection, EnProduct.Row row) {
        var en = EnProduct.INSTANCE;

        if (row.id() == null) {
            row.id(en.nextSeqProduct(connection));
        }

        var sql = new SqlList();
        var places = new ArrayList<String>();

        sql.ln(INSERT_INTO, en.tableName(), '(');
        sql.ln(' ', en.id);
        places.add(param("id", row.id()));

        if (row.isNameChanged()) {
            sql.ln(',', en.name);
            places.add(param("name", row.name()));
        }

        if (row.isPriceChanged()) {
            sql.ln(',', en.price);
            places.add(param("price", row.price()));
        }

        if (row.isDescriptionChanged()) {
            sql.ln(',', en.description);
            places.add(param("description", row.description()));
        }

        if (row.isImageChanged()) {
            sql.ln(',', en.image);
            places.add(param("image", row.image()));
        }

        sql.ln(")");

        sql.ln(VALUES);
        sql.ln("(" + StringUtils.join(places, ",") + ")");

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
