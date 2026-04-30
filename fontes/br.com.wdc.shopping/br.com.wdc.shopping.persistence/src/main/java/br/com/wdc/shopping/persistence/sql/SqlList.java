package br.com.wdc.shopping.persistence.sql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.function.ThrowingFunction;

public class SqlList extends ArrayList<String> {

    private static final long serialVersionUID = -8984873878336730817L;

    public static SqlList create(Consumer<SqlList> builder) {
        var sql = new SqlList();
        builder.accept(sql);
        return sql;
    }

    public SqlList() {
        super();
    }

    public SqlList(Collection<String> c) {
        super(c);
    }

    public SqlList(int initialCapacity) {
        super(initialCapacity);
    }

    public SqlList ln(Object... itens) {
        this.add(StringUtils.join(itens, ' '));
        return this;
    }

    // :: Projection

    private int projectionCount;

    private String projectionToLn(int columnIndex, Object... itens) {
        var sb = new StringBuilder();
        sb.append(columnIndex == 1 ? " " : ",");
        sb.append(StringUtils.join(itens, ' '));
        return sb.toString();
    }

    public SqlList field(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return this;
    }

    public ThrowingFunction<ResultSet, Boolean> bitColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, Byte> i08Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getByte(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, Short> i16Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getShort(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, Integer> i32Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getInt(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, Long> i64Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getLong(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, BigInteger> intColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getBigDecimal(columnIndex);
            return val == null ? null : val.toBigInteger();
        };
    }

    public ThrowingFunction<ResultSet, Float> f32Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getFloat(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, Double> f64Column(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> {
            var val = rs.getDouble(columnIndex);
            return rs.wasNull() ? null : val;
        };
    }

    public ThrowingFunction<ResultSet, BigDecimal> decColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getBigDecimal(columnIndex);
    }

    public ThrowingFunction<ResultSet, byte[]> binColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getBytes(columnIndex);
    }

    public ThrowingFunction<ResultSet, String> strColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getString(columnIndex);
    }

    public ThrowingFunction<ResultSet, java.sql.Date> dteColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getDate(columnIndex);
    }

    public ThrowingFunction<ResultSet, java.sql.Time> tmeColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getTime(columnIndex);
    }

    public ThrowingFunction<ResultSet, java.sql.Timestamp> dttColumn(Object... itens) {
        var columnIndex = ++this.projectionCount;
        this.ln(projectionToLn(columnIndex, itens));
        return rs -> rs.getTimestamp(columnIndex);
    }

    public String toText() {
        return this.toText(0);
    }

    public String toText(int identLength) {
        if (this.isEmpty()) {
            return "";
        }

        String ident = StringUtils.EMPTY;
        if (identLength > 0) {
            ident = StringUtils.repeat(' ', identLength);
        }

        return this.toText(ident);
    }

    public String toText(String ident) {
        // Não usar StringUtils.join nem stream.collector :: geram linha em branco no SQL
        StringBuilder sb = new StringBuilder(this.size() * 160);

        var br = "";
        for (String line : this) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            sb.append(br);
            sb.append(ident);
            sb.append(line);
            br = "\n";
        }
        return sb.toString();
    }

}
