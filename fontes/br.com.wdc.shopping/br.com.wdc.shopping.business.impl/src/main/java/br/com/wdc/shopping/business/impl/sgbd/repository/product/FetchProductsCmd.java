package br.com.wdc.shopping.business.impl.sgbd.repository.product;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.impl.sgbd.utils.DbField;
import br.com.wdc.shopping.business.impl.sgbd.utils.SqlUtils;
import br.com.wdc.shopping.business.shared.criteria.ProductCriteria;
import br.com.wdc.shopping.business.shared.model.Product;
import br.com.wdc.shopping.business.shared.utils.ProjectionValues;

public class FetchProductsCmd extends BaseCommand {

    // :: Class API

    public static Product byId(Connection connection, Long productId, Product projection) {
        if (productId == null) {
            throw new AssertionError("productId is required");
        }

        var list = byCriteria(connection, new ProductCriteria()
                .withProductId(productId)
                .withProjection(projection));
        return list.isEmpty() ? null : list.get(0);
    }

    public static List<Product> byCriteria(Connection connection, ProductCriteria criteria) {
        return new FetchProductsCmd().execute(connection, criteria);
    }

    // :: Action

    public List<Product> execute(Connection connection, ProductCriteria criteria) {
        var sql = new SqlList();

        var cteProduct = new EnProduct("cteProduct");
        sql.ln(WITH, cteProduct.alias(), AS, '(');
        sql.ln(this.cteProduct(criteria, criteria.projection(), null, null).toText("  "));
        sql.ln(')');
        sql.ln(SELECT);

        var fields = fields(criteria.projection(), cteProduct);
        var fJsonData = sql.strColumn(SqlUtils.toJsonField(fields), AS, "json_data");
        sql.ln(FROM, cteProduct.alias());

        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);

            var productMap = new HashMap<Long, Product>();
            return query.map((rs, _) -> fromJson(fJsonData.apply(rs), productMap)).list();
        }
    }

    public SqlList cteProduct(ProductCriteria criteria, Product prj, String superAlias, DbField superId) {
        var p = new EnProduct("P");

        var sql = new SqlList();
        sql.ln(SELECT);
        fields(prj, p).forEach(sql::field);
        sql.ln(FROM, p.tableRef());
        sql.ln(WHERE_TRUE);

        if (superAlias != null) {
            sql.ln(AND, EXISTS(ll -> ll
                    .ln(SELECT, 1)
                    .ln(FROM, superAlias)
                    .ln(WHERE, superId, EQUAL, p.id)));
        }

        if (criteria == null) {
            return sql;
        }

        var applier = new ApplyProductCriteria(this);
        applier.criteria = criteria;
        applier.root = p;
        applier.apply(sql);

        if (criteria.orderBy() != null) {
            switch (criteria.orderBy()) {
            case ACENDING -> sql.ln(ORDER_BY(p.id.asc()));
            case DESCENDING -> sql.ln(ORDER_BY(p.id.desc()));
            }
        }

        if (criteria.limit() != null) {
            sql.ln(LIMIT, criteria.limit());
        }

        if (criteria.offset() != null) {
            sql.ln(OFFSET, criteria.offset());
        }

        return sql;
    }

    // :: Public Class API

    public static List<DbField> fields(Product prj, EnProduct en) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new Product();
            prj.name = pv.str;
            prj.price = pv.f64;
            prj.description = pv.str;
        }
        prj.id = pv.i64;

        var fields = new ArrayList<DbField>();

        if (prj.id != null) {
            fields.add(en.id);
        }

        if (prj.name != null) {
            fields.add(en.name);
        }

        if (prj.price != null) {
            fields.add(en.price);
        }

        if (prj.description != null) {
            fields.add(en.description);
        }

        if (prj.image != null) {
            fields.add(en.image);
        }

        return fields;
    }

    public static Product fromJson(String json, Map<Long, Product> productMap) {
        try (var reader = new JsonReader(new StringReader(json))) {
            var row = EnProduct.Row.parseJson(reader);

            var product = productMap.computeIfAbsent(row.id(), k -> {
                var bean = new Product();
                bean.id = k;
                return bean;
            });

            product.name = row.name();
            product.description = row.description();
            product.image = row.image();
            product.price = CoerceUtils.asDouble(row.price());

            return product;
        } catch (IOException caught) {
            throw new UncheckedIOException(caught);
        }
    }

}
