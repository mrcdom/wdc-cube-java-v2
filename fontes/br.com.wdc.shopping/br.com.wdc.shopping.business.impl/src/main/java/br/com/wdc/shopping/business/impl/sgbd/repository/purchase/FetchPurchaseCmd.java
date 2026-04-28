package br.com.wdc.shopping.business.impl.sgbd.repository.purchase;

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

import br.com.wdc.framework.commons.function.ThrowingConsumer;
import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.repository.product.FetchProductsCmd;
import br.com.wdc.shopping.business.impl.sgbd.repository.purchaseitem.FetchPurchaseItemsCmd;
import br.com.wdc.shopping.business.impl.sgbd.repository.user.FetchUsersCmd;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.impl.sgbd.utils.DbField;
import br.com.wdc.shopping.business.impl.sgbd.utils.SqlUtils;
import br.com.wdc.shopping.business.shared.criteria.PurchaseCriteria;
import br.com.wdc.shopping.business.shared.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.business.shared.model.Product;
import br.com.wdc.shopping.business.shared.model.Purchase;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;
import br.com.wdc.shopping.business.shared.model.User;
import br.com.wdc.shopping.business.shared.utils.ProjectionList;
import br.com.wdc.shopping.business.shared.utils.ProjectionValues;

public class FetchPurchaseCmd extends BaseCommand {

    public static Purchase byId(Connection connection, Long purchaseId, Purchase projection) {
        if (purchaseId == null) {
            throw new AssertionError("purchaseId is required");
        }

        var list = byCriteria(connection, new PurchaseCriteria()
                .withPurchaseId(purchaseId)
                .withProjection(projection));

        return list.isEmpty() ? null : list.get(0);

    }

    public static List<Purchase> byCriteria(Connection connection, PurchaseCriteria criteria) {
        return new FetchPurchaseCmd().execute(connection, criteria);
    }

    // :: Action

    public List<Purchase> execute(Connection connection, PurchaseCriteria criteria) {
        if (criteria == null) {
            criteria = new PurchaseCriteria();
        }

        var sql = this.buildCte(criteria);

        var fJsonData = "json_data";
        var unionAll = "         ";

        final var cteUserId = 1;
        if (this.userPrj != null) {
            var expr = SqlUtils.toJsonField(FetchUsersCmd.fields(this.userPrj, this.cteUser));
            sql.ln(unionAll, SELECT, cteUserId + ",", expr, AS, fJsonData, FROM, this.cteUser.alias());
            unionAll = UNION_ALL;
        }

        final var cteProductId = 2;
        if (this.cteProduct != null) {
            var expr = SqlUtils.toJsonField(FetchProductsCmd.fields(this.productPrj, this.cteProduct));
            sql.ln(unionAll, SELECT, cteProductId + ",", expr, AS, fJsonData, FROM, this.cteProduct.alias());
            unionAll = UNION_ALL;
        }

        final var ctePurchaseId = 3;
        if (this.ctePurchase != null) {
            var expr = SqlUtils.toJsonField(FetchPurchaseCmd.fields(this.purchasePrj, this.ctePurchase));
            sql.ln(unionAll, SELECT, ctePurchaseId + ",", expr, AS, fJsonData, FROM, this.ctePurchase.alias());
            unionAll = UNION_ALL;
        }

        final var ctePurchaseItemId = 4;
        if (this.ctePurchaseItem != null) {
            var expr = SqlUtils.toJsonField(FetchPurchaseItemsCmd.fields(this.purchaseItemPrj, this.ctePurchaseItem));
            sql.ln(unionAll, SELECT, ctePurchaseItemId + ",", expr, AS, fJsonData, FROM,
                    this.ctePurchaseItem.alias());
        }

        // Reference Maps
        var userMap = new HashMap<Long, User>();
        var productMap = new HashMap<Long, Product>();
        var purchaceMap = new HashMap<Long, Purchase>();
        var purchaceItemMap = new HashMap<Long, PurchaseItem>();

        // Result List
        var purchaseList = new ArrayList<Purchase>();

        // Read content
        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);

            query.map((rs, _) -> {
                var cteId = rs.getInt(1);
                var jsonData = rs.getString(2);

                switch (cteId) {
                case cteUserId -> FetchUsersCmd.fromJson(jsonData, userMap);
                case cteProductId -> FetchProductsCmd.fromJson(jsonData, productMap);
                case ctePurchaseId -> purchaseList.add(fromJson(jsonData, purchaceMap, userMap));
                case ctePurchaseItemId -> FetchPurchaseItemsCmd.fromJson(jsonData, purchaceItemMap,
                        purchaceMap, productMap);
                default -> ThrowingRunnable.noop();
                }

                return Boolean.TRUE;
            }).forEach(ThrowingConsumer.noop());
        }

        return purchaseList;
    }

    protected Purchase safeProjection(Purchase prj) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new Purchase();
            prj.buyDate = pv.offsetDateTime;
        }
        prj.id = pv.i64;

        // Make sure FK is present

        if (prj.user != null) {
            prj.user.id = pv.i64;
        }

        return prj;
    }

    private SqlList buildCte(PurchaseCriteria criteria) {

        var ident = "  ";

        var sql = new SqlList();

        this.ctePurchase = new EnPurchase("ctePurchase");
        this.purchasePrj = this.safeProjection(criteria.projection());

        sql.ln(WITH, this.ctePurchase.alias(), AS, '(');
        sql.ln(this.ctePurchase(criteria, this.purchasePrj, null, null).toText(ident));
        sql.ln(")");

        if (this.purchasePrj.user != null) {
            this.userPrj = this.purchasePrj.user;
            this.cteUser = new EnUser("cteUser");

            sql.ln(",", this.cteUser.alias(), AS, '(');
            sql.ln(this.cteUser(this.userPrj, this.ctePurchase).toText(ident));
            sql.ln(")");
        }

        if (this.purchasePrj.items != null && !this.purchasePrj.items.isEmpty()) {
            this.purchaseItemPrj = this.purchasePrj.items.get(0);

            // Avoid circular reference and assure FK
            this.purchaseItemPrj.purchase = new Purchase();

            var itemCriteria = PurchaseItemCriteria.class.cast(null);
            if (this.purchasePrj.items instanceof ProjectionList<?> list) {
                itemCriteria = (PurchaseItemCriteria) list.getCriteria();
            }

            this.ctePurchaseItem = new EnPurchaseItem("ctePurchaseItem");
            sql.ln(",", this.ctePurchaseItem.alias(), AS, '(');
            sql.ln(this.ctePurchaseItem(itemCriteria, this.purchaseItemPrj, this.ctePurchase).toText(ident));
            sql.ln(")");

            if (this.purchaseItemPrj.product != null) {
                this.cteProduct = new EnProduct("cteProduct");
                this.productPrj = this.purchaseItemPrj.product;

                sql.ln(",", this.cteProduct.alias(), AS, '(');
                sql.ln(this.cteProduct(this.productPrj, this.ctePurchaseItem).toText(ident));
                sql.ln(")");
            }
        }

        return sql;
    }

    protected EnPurchase ctePurchase;
    protected Purchase purchasePrj;

    public SqlList ctePurchase(PurchaseCriteria criteria, Purchase prj, String superAlias, DbField superId) {
        var b = new EnPurchase("B");

        var sql = new SqlList();

        sql.ln(SELECT);
        fields(prj, b).forEach(sql::field);
        sql.ln(FROM, b.tableRef());
        sql.ln(WHERE_TRUE);

        if (superAlias != null) {
            sql.ln(AND, this.EXISTS(ll -> ll
                    .ln(SELECT, 1)
                    .ln(FROM, superAlias)
                    .ln(WHERE, superId, EQUAL, b.id)));
        }

        if (criteria == null) {
            return sql;
        }

        var applier = new ApplyPurshaseCriteria(this);
        applier.criteria = criteria;
        applier.root = b;
        applier.apply(sql);

        if (criteria.orderBy() != null) {
            switch (criteria.orderBy()) {
            case ACENDING -> sql.ln(this.ORDER_BY(b.id.asc()));
            case DESCENDING -> sql.ln(this.ORDER_BY(b.id.desc()));
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

    protected EnPurchaseItem ctePurchaseItem;
    protected PurchaseItem purchaseItemPrj;

    private SqlList ctePurchaseItem(PurchaseItemCriteria criteria, PurchaseItem prj, EnPurchase owner) {
        return new FetchPurchaseItemsCmd().ctePurchaseItem(criteria, prj, owner.alias(), owner.id);
    }

    protected EnProduct cteProduct;
    protected Product productPrj;

    private SqlList cteProduct(Product prj, EnPurchaseItem owner) {
        return new FetchProductsCmd().cteProduct(null, prj, owner.alias(), owner.productId);
    }

    protected EnUser cteUser;
    protected User userPrj;

    private SqlList cteUser(User prj, EnPurchase owner) {
        return new FetchUsersCmd().cteUser(null, prj, owner.alias(), owner.userId);
    }

    // :: Public Class API

    public static List<DbField> fields(Purchase prj, EnPurchase en) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new Purchase();
            prj.buyDate = pv.offsetDateTime;
        }

        prj.id = pv.i64;

        var fields = new ArrayList<DbField>();
        fields.add(en.id);

        if (prj.buyDate != null) {
            fields.add(en.buyDate);
        }

        if (prj.user != null) {
            fields.add(en.userId);
        }

        return fields;
    }

    public static Purchase fromJson(String json, Map<Long, Purchase> purchaceMap, Map<Long, User> userMap) {
        try (var reader = new JsonReader(new StringReader(json))) {
            var row = EnPurchase.Row.parseJson(reader);

            var purchase = purchaceMap.computeIfAbsent(row.id(), k -> {
                var bean = new Purchase();
                bean.id = k;
                return bean;
            });

            if (purchase.buyDate == null) {
                purchase.buyDate = row.buyDate();
            }

            if (row.userId() != null && purchase.user == null) {
                purchase.user = userMap.get(row.userId());
                if (purchase.user == null) {
                    purchase.user = new User();
                    purchase.user.id = row.userId();
                    userMap.put(row.userId(), purchase.user);
                }
            }

            return purchase;
        } catch (IOException caught) {
            throw new UncheckedIOException(caught);
        }
    }

}
