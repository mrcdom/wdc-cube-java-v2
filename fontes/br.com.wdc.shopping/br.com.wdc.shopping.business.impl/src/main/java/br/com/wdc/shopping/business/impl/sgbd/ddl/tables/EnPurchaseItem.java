package br.com.wdc.shopping.business.impl.sgbd.ddl.tables;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.gson.JsonCoerceUtils;
import br.com.wdc.framework.commons.gson.JsonReaderHelper;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseRow;
import br.com.wdc.shopping.business.impl.sgbd.utils.DbField;
import br.com.wdc.shopping.business.impl.sgbd.utils.DbTable;
import br.com.wdc.shopping.business.impl.sgbd.utils.SqlUtils;

public class EnPurchaseItem extends DbTable {

    public static final EnPurchaseItem INSTANCE = new EnPurchaseItem("");

    public final DbField id;
    public final DbField purchaseId;
    public final DbField productId;
    public final DbField amount;
    public final DbField price;

    private final List<DbField> fields;

    public EnPurchaseItem(String alias) {
        super(alias);

        this.id = mkBigint("ID", false);
        this.purchaseId = mkBigint("PURCHASEID", false);
        this.productId = mkBigint("PRODUCTID", false);
        this.amount = mkInt("AMOUNT", false);
        this.price = mkNumeric("PRICE", 20, 2, false);

        this.fields = Arrays.asList(id, purchaseId, productId, amount, price);
    }

    @Override
    public String tableName() {
        return "EN_PURCHASEITEM";
    }

    @Override
    public List<DbField> fields() {
        return fields;
    }

    @Override
    public String createTableSql() {
        var sql = new StringBuilderWriter();

        var baseName = this.tableName().substring(3);
        var enProduct = EnProduct.INSTANCE;
        var enPurchase = EnPurchase.INSTANCE;

        try (var out = new PrintWriter(sql)) {
            out.println("CREATE TABLE IF NOT EXISTS " + tableName() + " (");
            out.println(" " + this.id.declaration());
            out.println("," + this.purchaseId.declaration());
            out.println("," + this.productId.declaration());
            out.println("," + this.amount.declaration());
            out.println("," + this.price.declaration());

            out.println(",CONSTRAINT PK_" + baseName + " PRIMARY KEY (" + this.id.name() + ")");

            var ident = "                             ";

            out.println(",CONSTRAINT FK_" + baseName + "_PRODUCT FOREIGN KEY (" + this.productId.name() + ")");
            out.println(ident + "REFERENCES " + enProduct.tableName() + "(" + enProduct.id.name() + ")");

            out.println(",CONSTRAINT FK_" + baseName + "_PURCHASE FOREIGN KEY (" + this.purchaseId.name() + ")");
            out.println(ident + "REFERENCES " + enPurchase.tableName() + "(" + enPurchase.id.name() + ")");

            out.println(")");
        }

        return sql.toString();
    }

    @Override
    public String createSequeceSql() {
        return "CREATE SEQUENCE IF NOT EXISTS SQ_PURCHASEITEM START WITH 1 INCREMENT BY 1";
    }

    public Long nextSeqPurchaseItem(Connection connection) {
        return SqlUtils.nextSequence(connection, "SQ_PURCHASEITEM");
    }

    public void alterSeqPurchaseItem(Connection connection, long value) {
        SqlUtils.alterSequence(connection, "SQ_PURCHASEITEM", value);
    }

    public static class Row extends BaseRow {
        // :: ID

        private Long id;
        private boolean idChanged;

        public Long id() {
            return this.id;
        }

        public Row id(Long value) {
            this.id = value;
            this.idChanged = true;
            return this;
        }

        public boolean isIdChanged() {
            return idChanged;
        }

        // :: PURCHASEID

        private Long purchaseId;
        private boolean purchaseIdChanged;

        public Long purchaseId() {
            return this.purchaseId;
        }

        public Row purchaseId(Long value) {
            this.purchaseId = value;
            this.purchaseIdChanged = true;
            return this;
        }

        public boolean isPurchaseIdChanged() {
            return purchaseIdChanged;
        }

        // :: PURCHASEID

        private Long productId;
        private boolean productIdChanged;

        public Long productId() {
            return this.productId;
        }

        public Row productId(Long value) {
            this.productId = value;
            this.productIdChanged = true;
            return this;
        }

        public boolean isProductIdChanged() {
            return productIdChanged;
        }

        // :: AMOUNT

        private Integer amount;
        private boolean amountChanged;

        public Integer amount() {
            return this.amount;
        }

        public Row amount(Integer value) {
            this.amount = value;
            this.amountChanged = true;
            return this;
        }

        public boolean isAmountChanged() {
            return amountChanged;
        }

        // :: PRICE

        private BigDecimal price;
        private boolean priceChanged;

        public BigDecimal price() {
            return this.price;
        }

        public Row price(BigDecimal value) {
            this.price = value;
            this.priceChanged = true;
            return this;
        }

        public boolean isPriceChanged() {
            return priceChanged;
        }

        // :: BaseRow API

        @Override
        public void clearChanges() {
            this.idChanged = false;
            this.purchaseIdChanged = false;
            this.productIdChanged = false;
            this.amountChanged = false;
            this.priceChanged = false;
        }

        public static Row parseJson(JsonReader reader) throws IOException {
            var row = new EnPurchaseItem.Row();

            new JsonReaderHelper(reader).object(obj0 -> {
                var en = EnPurchaseItem.INSTANCE;

                obj0.put(en.id.name(), () -> row.id(JsonCoerceUtils.asLong(reader)));
                obj0.put(en.amount.name(), () -> row.amount(JsonCoerceUtils.asInteger(reader)));
                obj0.put(en.price.name(), () -> row.price(JsonCoerceUtils.asBigDecimal(reader)));
                obj0.put(en.purchaseId.name(), () -> row.purchaseId(JsonCoerceUtils.asLong(reader)));
                obj0.put(en.productId.name(), () -> row.productId(JsonCoerceUtils.asLong(reader)));
            });

            return row;
        }
    }
}
