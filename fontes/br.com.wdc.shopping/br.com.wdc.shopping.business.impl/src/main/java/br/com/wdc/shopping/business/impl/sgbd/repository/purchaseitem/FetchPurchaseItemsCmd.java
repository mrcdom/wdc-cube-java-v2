package br.com.wdc.shopping.business.impl.sgbd.repository.purchaseitem;

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
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnProduct;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchase;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnPurchaseItem;
import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.repository.product.FetchProductsCmd;
import br.com.wdc.shopping.business.impl.sgbd.repository.purchase.FetchPurchaseCmd;
import br.com.wdc.shopping.business.impl.sgbd.repository.user.FetchUsersCmd;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.impl.sgbd.utils.DbField;
import br.com.wdc.shopping.business.impl.sgbd.utils.SqlUtils;
import br.com.wdc.shopping.business.shared.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.business.shared.model.Product;
import br.com.wdc.shopping.business.shared.model.Purchase;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;
import br.com.wdc.shopping.business.shared.model.User;
import br.com.wdc.shopping.business.shared.utils.ProjectionValues;

public class FetchPurchaseItemsCmd extends BaseCommand {

    public static PurchaseItem byId(Connection connection, Long purchaseItemId, PurchaseItem projection) {
        if (purchaseItemId == null) {
            throw new AssertionError("purchaseItemId is required");
        }

        var list = new FetchPurchaseItemsCmd().execute(connection, new PurchaseItemCriteria()
                .withPurchaseItemId(purchaseItemId)
                .withProjection(projection));

        return list.isEmpty() ? null : list.get(0);

    }

    public static List<PurchaseItem> byCriteria(Connection connection, PurchaseItemCriteria criteria) {
        return new FetchPurchaseItemsCmd().execute(connection, criteria);
    }

    // :: Action

    public List<PurchaseItem> execute(Connection connection, PurchaseItemCriteria criteria) {
        if (criteria == null) {
            criteria = new PurchaseItemCriteria();
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
            sql.ln(unionAll, SELECT, ctePurchaseItemId + ",", expr, AS, fJsonData, FROM, this.ctePurchaseItem.alias());
        }

        // Reference Maps
        var userMap = new HashMap<Long, User>();
        var productMap = new HashMap<Long, Product>();
        var purchaceMap = new HashMap<Long, Purchase>();
        var purchaceItemMap = new HashMap<Long, PurchaseItem>();

        // Result List
        var purchaseItemList = new ArrayList<PurchaseItem>();

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
                case ctePurchaseId -> FetchPurchaseCmd.fromJson(jsonData, purchaceMap, userMap);
                case ctePurchaseItemId -> purchaseItemList
                        .add(FetchPurchaseItemsCmd.fromJson(jsonData, purchaceItemMap, purchaceMap, productMap));
                default -> ThrowingRunnable.noop();
                }

                return Boolean.TRUE;
            }).forEach(ThrowingConsumer.noop());
        }

        return purchaseItemList;
    }

    private PurchaseItem safeProjection(PurchaseItem prj) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new PurchaseItem();
            prj.amount = pv.i32;
            prj.price = pv.f64;
        }

        prj.id = pv.i64;

        return prj;
    }

    private SqlList buildCte(PurchaseItemCriteria criteria) {
        var ident = "  ";

        var sql = new SqlList();

        this.ctePurchaseItem = new EnPurchaseItem("ctePurchaseItem");
        this.purchaseItemPrj = safeProjection(criteria.projection());

        sql.ln(WITH, this.ctePurchaseItem.alias(), AS, '(');
        sql.ln(this.ctePurchaseItem(criteria, this.purchaseItemPrj, null, null).toText(ident));
        sql.ln(")");

        if (this.purchaseItemPrj.product != null) {
            this.cteProduct = new EnProduct("cteProduct");
            this.productPrj = this.purchaseItemPrj.product;

            sql.ln(",", this.cteProduct.alias(), AS, '(');
            sql.ln(this.cteProduct(this.productPrj, this.ctePurchaseItem).toText(ident));
            sql.ln(")");
        }

        if (this.purchaseItemPrj.purchase != null) {
            this.ctePurchase = new EnPurchase("ctePurchase");
            this.purchasePrj = this.purchaseItemPrj.purchase;

            // Assure no circular reference
            this.purchasePrj.items = null;

            sql.ln(",", this.ctePurchase.alias(), AS, '(');
            sql.ln(this.ctePurchase(this.purchasePrj, this.ctePurchaseItem).toText(ident));
            sql.ln(")");

            if (this.purchasePrj.user != null) {
                this.userPrj = this.purchasePrj.user;
                this.cteUser = new EnUser("cteUser");

                sql.ln(",", this.cteUser.alias(), AS, '(');
                sql.ln(this.cteUser(this.userPrj, this.ctePurchase).toText(ident));
                sql.ln(")");
            }
        }

        return sql;
    }

    protected EnPurchaseItem ctePurchaseItem;
    protected PurchaseItem purchaseItemPrj;

    public SqlList ctePurchaseItem(PurchaseItemCriteria criteria, PurchaseItem prj, String ownerAlias,
            DbField ownerId) {
        var pi = new EnPurchaseItem("PI");

        var sql = new SqlList();
        sql.ln(SELECT);
        fields(prj, pi).forEach(sql::field);
        sql.ln(FROM, pi.tableRef());
        sql.ln(WHERE_TRUE);

        if (ownerAlias != null) {
            sql.ln(AND, this.EXISTS(ll -> ll
                    .ln(SELECT, 1)
                    .ln(FROM, ownerAlias)
                    .ln(WHERE, ownerId, EQUAL, pi.purchaseId)));
        }

        if (criteria == null) {
            return sql;
        }

        var applier = new ApplyPurshaseItemCriteria(this);
        applier.criteria = criteria;
        applier.root = pi;
        applier.apply(sql);

        if (criteria.orderBy() != null) {
            switch (criteria.orderBy()) {
            case ACENDING -> sql.ln(this.ORDER_BY(pi.id.asc()));
            case DESCENDING -> sql.ln(this.ORDER_BY(pi.id.desc()));
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

    protected EnProduct cteProduct;
    protected Product productPrj;

    private SqlList cteProduct(Product prj, EnPurchaseItem superEn) {
        return new FetchProductsCmd().cteProduct(null, prj, superEn.alias(), superEn.productId);
    }

    protected EnPurchase ctePurchase;
    protected Purchase purchasePrj;

    public SqlList ctePurchase(Purchase prj, EnPurchaseItem superEn) {
        var pv = ProjectionValues.INSTANCE;

        // Assure FK
        if (prj.user != null) {
            prj.user.id = pv.i64;
        }

        return new FetchPurchaseCmd().ctePurchase(null, prj, superEn.alias(), superEn.purchaseId);
    }

    protected EnUser cteUser;
    protected User userPrj;

    private SqlList cteUser(User prj, EnPurchase superEn) {
        return new FetchUsersCmd().cteUser(null, prj, superEn.alias(), superEn.userId);
    }

    // :: Public Class API

    public static List<DbField> fields(PurchaseItem prj, EnPurchaseItem en) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new PurchaseItem();
            prj.amount = pv.i32;
            prj.price = pv.f64;
        }

        prj.id = pv.i64;

        var fields = new ArrayList<DbField>();
        if (prj.id != null) {
            fields.add(en.id);
        }

        if (prj.amount != null) {
            fields.add(en.amount);
        }

        if (prj.price != null) {
            fields.add(en.price);
        }

        if (prj.product != null) {
            fields.add(en.productId);
        }

        if (prj.purchase != null) {
            fields.add(en.purchaseId);
        }

        return fields;
    }

    public static PurchaseItem fromJson(String json, Map<Long, PurchaseItem> purchaceItemMap,
            Map<Long, Purchase> purchaceMap,
            Map<Long, Product> productMap) {
        try (var reader = new JsonReader(new StringReader(json))) {
            var row = EnPurchaseItem.Row.parseJson(reader);

            var purchaseItem = purchaceItemMap.computeIfAbsent(row.id(), k -> {
                var bean = new PurchaseItem();
                bean.id = k;
                return bean;
            });

            purchaseItem.amount = row.amount();
            purchaseItem.price = CoerceUtils.asDouble(row.price());

            if (row.purchaseId() != null) {
                purchaseItem.purchase = purchaceMap.get(row.purchaseId());
                if (purchaseItem.purchase == null) {
                    purchaseItem.purchase = new Purchase();
                    purchaseItem.purchase.id = row.purchaseId();
                    purchaceMap.put(row.purchaseId(), purchaseItem.purchase);
                }

                if (purchaseItem.purchase.items == null) {
                    purchaseItem.purchase.items = new ArrayList<>();
                }
                purchaseItem.purchase.items.add(purchaseItem);
            }

            if (row.productId() != null) {
                purchaseItem.product = productMap.get(row.productId());
                if (purchaseItem.product == null) {
                    purchaseItem.product = new Product();
                    purchaseItem.product.id = row.productId();
                    productMap.put(row.productId(), purchaseItem.product);
                }
            }

            return purchaseItem;
        } catch (IOException caught) {
            throw new UncheckedIOException(caught);
        }
    }

}
