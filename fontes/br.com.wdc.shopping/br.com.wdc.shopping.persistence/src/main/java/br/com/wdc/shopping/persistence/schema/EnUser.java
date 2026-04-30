package br.com.wdc.shopping.persistence.schema;

import br.com.wdc.shopping.persistence.schema.support.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.StringBuilderWriter;

import com.google.gson.stream.JsonReader;

import br.com.wdc.framework.commons.gson.JsonCoerceUtils;
import br.com.wdc.framework.commons.gson.JsonReaderHelper;
import br.com.wdc.shopping.persistence.sql.SqlUtils;

public class EnUser extends DbTable {

    public static final EnUser INSTANCE = new EnUser("");

    public final DbField id;
    public final DbField userName;
    public final DbField password;
    public final DbField name;

    private final List<DbField> fields;

    public EnUser(String alias) {
        super(alias);

        this.id = mkBigint("ID", false);
        this.userName = mkVarChar("USERNAME", 255, false);
        this.password = mkChar("PASSWORD", 32, false);
        this.name = mkVarChar("NAME", 255, false);

        this.fields = Arrays.asList(id, userName, password, name);
    }

    @Override
    public String tableName() {
        return "EN_USER";
    }

    @Override
    public List<DbField> fields() {
        return this.fields;
    }

    @Override
    public String createTableSql() {
        var sql = new StringBuilderWriter();

        var baseName = this.tableName().substring(3);

        try (var out = new PrintWriter(sql)) {
            out.println("CREATE TABLE IF NOT EXISTS " + this.tableName() + " (");
            out.println(" " + this.id.declaration());
            out.println("," + this.userName.declaration());
            out.println("," + this.password.declaration());
            out.println("," + this.name.declaration());
            out.println(",CONSTRAINT PK_" + baseName + " PRIMARY KEY (" + this.id.name() + ")");
            out.println(")");
        }

        return sql.toString();
    }

    @Override
    public String createSequeceSql() {
        return "CREATE SEQUENCE IF NOT EXISTS SQ_USER START WITH 1 INCREMENT BY 1";
    }

    public Long nextSeqUser(Connection connection) {
        return SqlUtils.nextSequence(connection, "SQ_USER");
    }

    public void alterSeqUser(Connection connection, long value) {
        SqlUtils.alterSequence(connection, "SQ_USER", value);
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

        // :: USERNAME

        private String userName;
        private boolean userNameChanged;

        public String userName() {
            return this.userName;
        }

        public Row userName(String value) {
            this.userName = value;
            this.userNameChanged = true;
            return this;
        }

        public boolean isUserNameChanged() {
            return userNameChanged;
        }

        // :: PASSWORD

        private String password;
        private boolean passwordChanged;

        public String password() {
            return this.password;
        }

        public Row password(String value) {
            this.password = value;
            this.passwordChanged = true;
            return this;
        }

        public boolean isPasswordChanged() {
            return passwordChanged;
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

        // :: BaseRow API

        @Override
        public void clearChanges() {
            this.idChanged = false;
            this.userNameChanged = false;
            this.passwordChanged = false;
            this.nameChanged = false;
        }

        public static Row parseJson(JsonReader reader) throws IOException {
            var row = new EnUser.Row();

            new JsonReaderHelper(reader).object(obj0 -> {
                var en = EnUser.INSTANCE;

                obj0.put(en.id.name(), () -> row.id(JsonCoerceUtils.asLong(reader)));
                obj0.put(en.userName.name(), () -> row.userName(JsonCoerceUtils.asString(reader)));
                obj0.put(en.password.name(), () -> row.password(JsonCoerceUtils.asString(reader)));
                obj0.put(en.name.name(), () -> row.name(JsonCoerceUtils.asString(reader)));
            });

            return row;
        }
    }
}
