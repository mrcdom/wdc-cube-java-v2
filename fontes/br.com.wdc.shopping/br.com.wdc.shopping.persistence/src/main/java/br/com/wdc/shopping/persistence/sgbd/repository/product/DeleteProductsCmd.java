package br.com.wdc.shopping.persistence.sgbd.repository.product;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.ProductCriteria;

public class DeleteProductsCmd extends BaseCommand {

    // :: Class API

    public static int byId(Connection connection, Long productId) {
        if (productId == null) {
            throw new AssertionError("purchaseId is required");
        }
        return new DeleteProductsCmd().execute(connection, new ProductCriteria().withProductId(productId));
    }

    public static int byCriteria(Connection connection, ProductCriteria criteria) {
        return new DeleteProductsCmd().execute(connection, criteria);
    }

    // :: Action

    protected int execute(Connection connection, ProductCriteria criteria) {
        if (criteria.productId() == null) {
            throw new AssertionError("Missing primary key");
        }

        var p = new EnProduct("p");

        var sql = new SqlList();
        sql.ln(DELETE);
        sql.ln(FROM, p.tableRef());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyProductCriteria(this);
        applier.criteria = criteria;
        applier.root = p;
        applier.apply(sql);

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
