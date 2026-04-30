package br.com.wdc.shopping.persistence.schema.support;

import java.sql.JDBCType;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

public abstract class DbTable {

	protected final String alias;
	protected final String basePath;

	protected DbTable(String pAlias) {
		this.alias = pAlias;
		this.basePath = StringUtils.isNotBlank(pAlias) ? pAlias + "." : "";
	}

	public abstract String tableName();
	
	public abstract List<DbField> fields();

	public abstract String createTableSql();

	public String createSequeceSql() {
		throw new NotImplementedException();
	}

	public String alias() {
		return alias;
	}

	public String tableRef() {
		return StringUtils.isNotBlank(alias) ? tableName() + " " + alias : tableName();
	}

	public String basePath() {
		return basePath;
	}

	protected DbField mkTinyint(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.TINYINT, false, 0, 0, nullable);
	}

	protected DbField mkSmallint(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.SMALLINT, false, 0, 0, nullable);
	}

	protected DbField mkInt(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.INTEGER, false, 0, 0, nullable);
	}

	protected DbField mkBigint(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.BIGINT, false, 0, 0, nullable);
	}

	protected DbField mkNumeric(String name, int precision, int scale, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.NUMERIC, false, precision, scale, nullable);
	}

	protected DbField mkDecimal(String name, int precision, int scale, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.DECIMAL, false, precision, scale, nullable);
	}

	protected DbField mkFloat(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.FLOAT, false, 0, 0, nullable);
	}

	protected DbField mkDouble(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.DOUBLE, false, 0, 0, nullable);
	}

	protected DbField mkBoolean(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.BOOLEAN, false, 0, 0, nullable);
	}

	protected DbField mkChar(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.CHAR, false, length, 0, nullable);
	}

	protected DbField mkCharIgnoreCase(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.CHAR, true, length, 0, nullable);
	}

	protected DbField mkVarChar(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.VARCHAR, false, length, 0, nullable);
	}

	protected DbField mkVarCharIgnoreCase(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.VARCHAR, true, length, 0, nullable);
	}

	protected DbField mkNChar(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.NCHAR, false, length, 0, nullable);
	}

	protected DbField mkNCharIgnoreCase(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.NCHAR, true, length, 0, nullable);
	}

	protected DbField mkNVarChar(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.NVARCHAR, false, length, 0, nullable);
	}

	protected DbField mkNVarCharIgnoreCase(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.NVARCHAR, true, length, 0, nullable);
	}

	protected DbField mkBinary(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.BINARY, false, length, 0, nullable);
	}

	protected DbField mkVarBinary(String name, int length, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.VARBINARY, false, length, 0, nullable);
	}

	protected DbField mkDate(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.DATE, false, 0, 0, nullable);
	}

	protected DbField mkTime(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.TIME, false, 0, 0, nullable);
	}

	protected DbField mkTimestamp(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.TIMESTAMP, false, 0, 0, nullable);
	}

	protected DbField mkClob(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.CLOB, false, 0, 0, nullable);
	}

	protected DbField mkBlob(String name, boolean nullable) {
		return new DbField(name, alias, basePath + name, JDBCType.BLOB, false, 0, 0, nullable);
	}

}
