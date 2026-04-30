package br.com.wdc.shopping.scripts.sgbd;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.shopping.persistence.schema.EnProduct;
import br.com.wdc.shopping.persistence.schema.EnPurchase;
import br.com.wdc.shopping.persistence.schema.EnPurchaseItem;
import br.com.wdc.shopping.persistence.schema.EnUser;

public class DBCreate {

	private Connection connection;

	boolean mustResetDb = false;

	public DBCreate withConnection(Connection connection) {
		this.connection = connection;
		return this;
	}

	public DBCreate withReset() {
		this.mustResetDb = true;
		return this;
	}

	public DBCreate run() throws SQLException {
		var tableMap = this.loadTableMap(this.connection.getMetaData());

		var enUser = EnUser.INSTANCE;
		if (!tableMap.containsKey("PUBLIC." + enUser.tableName())) {
			this.createTableUser();
			this.mustResetDb = true;
		}

		var enProduct = EnProduct.INSTANCE;
		if (!tableMap.containsKey("PUBLIC." + enProduct.tableName())) {
			this.createTableProduct();
			this.mustResetDb = true;
		}

		var enPurchase = EnPurchase.INSTANCE;
		if (!tableMap.containsKey("PUBLIC." + enPurchase.tableName())) {
			this.createTablePurchase();
			this.mustResetDb = true;
		}

		var enPurchaseItem = EnPurchaseItem.INSTANCE;
		if (!tableMap.containsKey("PUBLIC." + enPurchaseItem.tableName())) {
			this.createTablePurchaseItem();
			this.mustResetDb = true;
		}

		if (this.mustResetDb) {
			DBReset.run(this.connection);
		}
		return this;
	}

	private Map<String, Boolean> loadTableMap(DatabaseMetaData metaData) throws SQLException {
		var tableMap = new TreeMap<String, Boolean>(String.CASE_INSENSITIVE_ORDER);
		try (var rs = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
			while (rs.next()) {
				var tableSchem = rs.getString("TABLE_SCHEM");
				var tableName = rs.getString("TABLE_NAME");

				var sb = new StringBuilder();

				if (StringUtils.isNotBlank(tableSchem)) {
					sb.append(tableSchem);
					sb.append(".");
				}
				sb.append(tableName);

				tableMap.put(sb.toString(), Boolean.TRUE);
			}
		}
		return tableMap;
	}

	private void createTableUser() throws SQLException {
		var enUser = EnUser.INSTANCE;

		try (var stmt = this.connection.createStatement()) {
			stmt.execute(enUser.createTableSql());
			stmt.execute(enUser.createSequeceSql());
		}
	}

	private void createTableProduct() throws SQLException {
		var enProduct = EnProduct.INSTANCE;

		try (var stmt = this.connection.createStatement()) {
			stmt.execute(enProduct.createTableSql());
			stmt.execute(enProduct.createSequeceSql());
		}
	}

	private void createTablePurchase() throws SQLException {
		var enPurchase = EnPurchase.INSTANCE;

		try (var stmt = this.connection.createStatement()) {
			stmt.execute(enPurchase.createTableSql());
			stmt.execute(enPurchase.createSequeceSql());
		}
	}

	private void createTablePurchaseItem() throws SQLException {
		var enPurchaseItem = EnPurchaseItem.INSTANCE;

		try (var stmt = this.connection.createStatement()) {
			stmt.execute(enPurchaseItem.createTableSql());
			stmt.execute(enPurchaseItem.createSequeceSql());
		}
	}

}
