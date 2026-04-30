package br.com.wdc.shopping.persistence.schema.support;

import java.sql.JDBCType;

@SuppressWarnings("java:S107")
public class DbField {

	// :: Instance

	private String name;
	private String alias;
	private String path;
	private JDBCType type;
	private boolean ignoreCase;
	private int lengthOrPrecision;
	private int scale;
	private boolean nullable;
	private String declaration;

	public DbField(String name, String alias, String path, JDBCType type, boolean ignoreCase, int lengthOrPrecision, int scale, boolean nullable) {
		this.name = name;
		this.alias = alias;
		this.path = path;
		this.type = type;
		this.ignoreCase = ignoreCase;
		this.lengthOrPrecision = lengthOrPrecision;
		this.scale = scale;
		this.nullable = nullable;
		this.declaration = name + " " + computeSqlType(type, ignoreCase, lengthOrPrecision, scale) + " " + computeSqlNullable(nullable);
	}

	public String name() {
		return this.name;
	}

	public String alias() {
		return this.alias;
	}

	public String path() {
		return this.path;
	}

	public JDBCType type() {
		return this.type;
	}

	public boolean ignoreCase() {
		return this.ignoreCase;
	}

	/**
	 * @return The Length for strings
	 */
	public int length() {
		return this.lengthOrPrecision;
	}

	/**
	 * @return The maximum total number of decimal digits to be stored
	 */
	public int precision() {
		return this.lengthOrPrecision;
	}

	/**
	 * @return The maximum total number of decimal digits to be stored or Length for strings
	 */
	public int lengthOrPrecision() {
		return this.lengthOrPrecision;
	}

	/**
	 * @return The number of decimal digits that are stored to the right of the decimal point
	 */
	public int scale() {
		return this.scale;
	}

	public boolean nullable() {
		return this.nullable;
	}

	public String declaration() {
		return this.declaration;
	}
	
	// :: SQL Helpers

	@Override
	public String toString() {
		return this.path;
	}
	
	public String asc() {
		return this.path + " ASC";
	}

	public String desc() {
		return this.path + " DESC";
	}

	// :: Internal

	private static String computeSqlNullable(boolean nullable) {
		return nullable ? "" : "NOT NULL";
	}

	private static String computeSqlType(JDBCType type, boolean ignoreCase, int lengthOrPrecision, int scale) {
		return switch (type) {
			case CHAR -> computeIgnoreCase("CHAR", ignoreCase) + "(" + lengthOrPrecision + ")";
			case VARCHAR -> computeIgnoreCase("VARCHAR", ignoreCase) + "(" + lengthOrPrecision + ")";
			case NUMERIC -> "NUMERIC(" + lengthOrPrecision + "," + scale + ")";
			case DECIMAL -> "DECIMAL(" + lengthOrPrecision + "," + scale + ")";
			case VARBINARY -> "VARBINARY(" + lengthOrPrecision + ")";
			case NCHAR -> computeIgnoreCase("NCHAR", ignoreCase) + "(" + lengthOrPrecision + ")";
			case NVARCHAR -> computeIgnoreCase("NVARCHAR", ignoreCase) + "(" + lengthOrPrecision + ")";
			case BINARY -> "BINARY(" + lengthOrPrecision + ")";
			default -> type.name();
		};
	}

	private static String computeIgnoreCase(String typeName, boolean ignoreCase) {
		return ignoreCase ? typeName + "_IGNORECASE" : typeName;
	}

}
