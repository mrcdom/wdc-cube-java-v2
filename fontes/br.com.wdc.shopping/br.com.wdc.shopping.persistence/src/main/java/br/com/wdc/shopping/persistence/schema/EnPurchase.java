package br.com.wdc.shopping.persistence.schema;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.gson.JsonCoerceUtils;
import br.com.wdc.framework.commons.gson.JsonReaderHelper;
import br.com.wdc.shopping.persistence.schema.BaseRow;
import br.com.wdc.shopping.persistence.schema.DbField;
import br.com.wdc.shopping.persistence.schema.DbTable;
import br.com.wdc.shopping.persistence.sql.SqlUtils;

public class EnPurchase extends DbTable {

    public static final EnPurchase INSTANCE = new EnPurchase("");

    public final DbField id;
    public final DbField userId;
    public final DbField buyDate;

    private final List<DbField> fields;

    public EnPurchase(String alias) {
        super(alias);

        this.id = mkBigint("ID", false);
        this.userId = mkBigint("USERID", false);
        this.buyDate = mkDate("BUYDATE", false);

        this.fields = Arrays.asList(id, userId, buyDate);
    }

    @Override
    public String tableName() {
        return "EN_PURCHASE";
    }

    @Override
    public List<DbField> fields() {
        return fields;
    }

    @Override
    public String createTableSql() {
        var sql = new StringBuilderWriter();

        var baseName = this.tableName().substring(3);
        var enUser = EnUser.INSTANCE;

        try (var out = new PrintWriter(sql)) {
            out.println("CREATE TABLE IF NOT EXISTS " + tableName() + " (");
            out.println(" " + this.id.declaration());
            out.println("," + this.userId.declaration());
            out.println("," + this.buyDate.declaration());
            out.println(",CONSTRAINT PK_" + baseName + " PRIMARY KEY (" + this.id.name() + ")");
            out.println(",CONSTRAINT FK_" + baseName + "_USER FOREIGN KEY (" + this.userId.name() + ")");
            out.println("                             REFERENCES " + enUser.tableName() + "(" + enUser.id.name() + ")");
            out.println(")");
        }

        return sql.toString();
    }

    @Override
    public String createSequeceSql() {
        return "CREATE SEQUENCE IF NOT EXISTS SQ_PURCHASE START WITH 1 INCREMENT BY 1";
    }

    public Long nextSeqPurchase(Connection connection) {
        return SqlUtils.nextSequence(connection, "SQ_PURCHASE");
    }

    public void alterSeqPurchase(Connection connection, long value) {
        SqlUtils.alterSequence(connection, "SQ_PURCHASE", value);
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

        // :: USERID

        private Long userId;
        private boolean userIdChanged;

        public Long userId() {
            return this.userId;
        }

        public Row userId(Long value) {
            this.userId = value;
            this.userIdChanged = true;
            return this;
        }

        public boolean isUserIdChanged() {
            return userIdChanged;
        }

        // :: BUYDATE

        private OffsetDateTime buyDate;
        private boolean buyDateChanged;

        public OffsetDateTime buyDate() {
            return this.buyDate;
        }

        public Row buyDate(OffsetDateTime value) {
            this.buyDate = value;
            this.buyDateChanged = true;
            return this;
        }

        public boolean isBuyDateChanged() {
            return buyDateChanged;
        }

        // :: BaseRow API

        @Override
        public void clearChanges() {
            this.idChanged = false;
            this.userIdChanged = false;
            this.buyDateChanged = false;
        }
        
        public static Row parseJson(JsonReader reader) throws IOException {
            var row = new EnPurchase.Row();

            new JsonReaderHelper(reader).object(obj0 -> {
                var en = EnPurchase.INSTANCE;
                obj0.put(en.id.name(), () -> row.id(JsonCoerceUtils.asLong(reader)));
                obj0.put(en.buyDate.name(), () -> row.buyDate(JsonCoerceUtils.asOffsetDateTime(reader)));
                obj0.put(en.userId.name(), () -> row.userId(JsonCoerceUtils.asLong(reader)));
            });
            
            return row;
        }
    }
}
