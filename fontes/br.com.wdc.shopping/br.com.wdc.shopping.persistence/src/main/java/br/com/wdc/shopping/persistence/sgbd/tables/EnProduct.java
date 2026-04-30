package br.com.wdc.shopping.persistence.sgbd.tables;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.gson.JsonCoerceUtils;
import br.com.wdc.framework.commons.gson.JsonReaderHelper;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseRow;
import br.com.wdc.shopping.persistence.sgbd.utils.DbField;
import br.com.wdc.shopping.persistence.sgbd.utils.DbTable;
import br.com.wdc.shopping.persistence.sgbd.utils.SqlUtils;

public class EnProduct extends DbTable {

    public static final EnProduct INSTANCE = new EnProduct("");

    public final DbField id;
    public final DbField name;
    public final DbField price;
    public final DbField description;
    public final DbField image;

    private final List<DbField> fields;

    public EnProduct(String alias) {
        super(alias);

        this.id = mkBigint("ID", false);
        this.name = mkVarCharIgnoreCase("NAME", 1000000, false);
        this.price = mkNumeric("PRICE", 20, 2, false);
        this.description = mkBinary("DESCRIPTION", 1000000, false);
        this.image = mkBinary("IMAGE", 1000000, true);

        this.fields = Arrays.asList(id, name, price, description, image);
    }

    @Override
    public String tableName() {
        return "EN_PRODUCT";
    }

    @Override
    public List<DbField> fields() {
        return fields;
    }

    @Override
    public String createTableSql() {
        var sql = new StringBuilderWriter();

        var baseName = this.tableName().substring(3);

        try (var out = new PrintWriter(sql)) {
            out.println("CREATE TABLE IF NOT EXISTS " + tableName() + " (");
            out.println(" " + this.id.declaration());
            out.println("," + this.name.declaration());
            out.println("," + this.price.declaration());
            out.println("," + this.description.declaration());
            out.println("," + this.image.declaration());
            out.println(",CONSTRAINT PK_" + baseName + " PRIMARY KEY (" + this.id.name() + ")");
            out.println(")");
        }

        return sql.toString();
    }

    @Override
    public String createSequeceSql() {
        return "CREATE SEQUENCE IF NOT EXISTS SQ_PRODUCT START WITH 1 INCREMENT BY 1";
    }

    public Long nextSeqProduct(Connection connection) {
        return SqlUtils.nextSequence(connection, "SQ_PRODUCT");
    }

    public void alterSeqProduct(Connection connection, long value) {
        SqlUtils.alterSequence(connection, "SQ_PRODUCT", value);
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

        // :: NAME

        private String name;
        private boolean nameChanged;

        public String name() {
            return this.name;
        }

        public Row name(String value) {
            this.name = value;
            this.nameChanged = true;
            return this;
        }

        public boolean isNameChanged() {
            return nameChanged;
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

        // :: NAME

        private String description;
        private boolean descriptionChanged;

        public String description() {
            return this.description;
        }

        public Row description(String value) {
            this.description = value;
            this.descriptionChanged = true;
            return this;
        }

        public boolean isDescriptionChanged() {
            return descriptionChanged;
        }

        // :: IMAGE

        private byte[] image;
        private boolean imageChanged;

        public byte[] image() {
            return this.image;
        }

        public Row image(byte[] value) {
            this.image = value;
            this.imageChanged = true;
            return this;
        }

        public boolean isImageChanged() {
            return imageChanged;
        }

        // :: BaseRow API

        @Override
        public void clearChanges() {
            this.idChanged = false;
            this.nameChanged = false;
            this.priceChanged = false;
            this.descriptionChanged = false;
            this.imageChanged = false;
        }

        public static Row parseJson(JsonReader reader) throws IOException {
            var row = new EnProduct.Row();

            new JsonReaderHelper(reader).object(obj0 -> {
                var en = EnProduct.INSTANCE;

                obj0.put(en.id.name(), () -> row.id(JsonCoerceUtils.asLong(reader)));
                obj0.put(en.name.name(), () -> row.name(JsonCoerceUtils.asString(reader)));
                obj0.put(en.price.name(), () -> row.price(JsonCoerceUtils.asBigDecimal(reader)));
                obj0.put(en.description.name(), () -> {
                    var bytes = JsonCoerceUtils.asByteArrayFromHex(reader);
                    if (bytes != null) {
                        row.description(new String(bytes, StandardCharsets.UTF_8));
                    }
                });
                obj0.put(en.image.name(), () -> row.image(JsonCoerceUtils.asByteArrayFromHex(reader)));
            });
            
            return row;
        }
    }
}
