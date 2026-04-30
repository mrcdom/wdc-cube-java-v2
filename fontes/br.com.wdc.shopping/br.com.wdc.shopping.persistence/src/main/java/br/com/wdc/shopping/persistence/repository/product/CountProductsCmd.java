package br.com.wdc.shopping.persistence.repository.product;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnProduct;
import br.com.wdc.shopping.persistence.sql.SqlList;

public class CountProductsCmd extends BaseCommand {

    public static int byCriteria(Connection connection, ProductCriteria criteria) {
        return new CountProductsCmd().execute(connection, criteria);
    }

    // :: Action

    protected int execute(Connection connection, ProductCriteria criteria) {
        var en = EnProduct.INSTANCE;

        var sql = new SqlList();
        sql.ln(SELECT, COUNT("*"));
        sql.ln(FROM, en.tableRef());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyProductCriteria(this);
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
