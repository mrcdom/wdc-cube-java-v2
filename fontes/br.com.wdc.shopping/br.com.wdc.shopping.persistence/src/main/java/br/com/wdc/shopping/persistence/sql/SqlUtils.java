package br.com.wdc.shopping.persistence.sql;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableObject;

import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.shopping.persistence.schema.DbField;

public class SqlUtils {

    private SqlUtils() {
        super();
    }

    public static Supplier<String> comma() {
        var val = new MutableObject<String>(" ");
        return () -> {
            var response = val.get();
            val.setValue(",");
            return response;
        };
    }

    public static Long nextSequence(Connection connection, String sequenceName) {
        try (var defer = new Defer()) {
            var stmt = connection.createStatement();
            defer.push(stmt::close);

            var rs = stmt.executeQuery("SELECT NEXT VALUE FOR " + sequenceName);
            defer.push(rs::close);
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new SQLException("No value returned from sequece");
        } catch (SQLException caught) {
            throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    public static void alterSequence(Connection connection, String name, long value) {
        try (var stmt = connection.createStatement()) {
            stmt.execute("ALTER SEQUENCE " + name + " RESTART WITH " + value);
        } catch (SQLException caught) {
            throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    public static String toJsonField(List<DbField> fields) {
        var it = fields.iterator();
        if (!it.hasNext()) {
            return "null";
        }

        var field = it.next();

        var sb = new StringBuilder(fields.size() * 30);
        sb.append("JSON_OBJECT(");

        var looping = true;
        do {
            sb.append('\'');
            sb.append(field.name());
            sb.append("': ");

            if (field.type() == JDBCType.BINARY) {
                sb.append("RAWTOHEX(");
                sb.append(field.path());
                sb.append(")");
            } else {
                sb.append(field.path());
            }

            if (it.hasNext()) {
                sb.append(',');
                field = it.next();
                continue;
            }
            looping = false;
        } while (looping);
        sb.append(')');

        return sb.toString();
    }

}
