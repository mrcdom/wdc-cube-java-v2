package br.com.wdc.shopping.scripts.sgbd;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonDialect;

/**
 * Creates the database schema using jOOQ and runs pending migrations.
 *
 * <p>The DDL statements here are the single source of truth for the schema.
 * The jOOQ generated classes (in persistence.jooq) are derived from this via
 * {@link GenerateJooqSchema}.</p>
 */
public class DBCreate {

	private Connection connection;
	private DSLContext dsl;

	boolean mustResetDb = false;
	private boolean skipReset = false;

	public DBCreate withConnection(Connection connection) {
		this.connection = connection;
		this.dsl = DSL.using(connection, SQLDialect.H2);
		return this;
	}

	public DBCreate withReset() {
		this.mustResetDb = true;
		return this;
	}

	public DBCreate withSkipReset() {
		this.skipReset = true;
		return this;
	}

	public DBCreate run() throws SQLException {
		JsonDialect.of(org.jooq.SQLDialect.H2).initialize(this.connection);

		var existingTables = loadExistingTables();

		if (!existingTables.contains("EN_USER")) {
			createTableUser();
			this.mustResetDb = true;
		}

		if (!existingTables.contains("EN_PRODUCT")) {
			createTableProduct();
			this.mustResetDb = true;
		}

		if (!existingTables.contains("EN_PURCHASE")) {
			createTablePurchase();
			this.mustResetDb = true;
		}

		if (!existingTables.contains("EN_PURCHASEITEM")) {
			createTablePurchaseItem();
			this.mustResetDb = true;
		}

		if (this.mustResetDb && !this.skipReset) {
			DBReset.run();
		}

		return this;
	}

	private Set<String> loadExistingTables() {
		return dsl.meta().getTables().stream()
				.map(Table::getName)
				.map(String::toUpperCase)
				.collect(Collectors.toSet());
	}

	// -- DDL definitions (source of truth) --

	private void createTableUser() {
		dsl.execute("""
				CREATE TABLE EN_USER (
				    ID BIGINT NOT NULL,
				    USERNAME VARCHAR(255) NOT NULL,
				    PASSWORD CHAR(32) NOT NULL,
				    NAME VARCHAR(255) NOT NULL,
				    ROLES VARCHAR(255),
				    CONSTRAINT PK_USER PRIMARY KEY (ID)
				)""");
		dsl.execute("CREATE SEQUENCE SQ_USER START WITH 1 INCREMENT BY 1");
	}

	private void createTableProduct() {
		dsl.execute("""
				CREATE TABLE EN_PRODUCT (
				    ID BIGINT NOT NULL,
				    NAME VARCHAR_IGNORECASE(1000000) NOT NULL,
				    PRICE NUMERIC(20,2) NOT NULL,
				    DESCRIPTION VARCHAR(1000000) NOT NULL,
				    IMAGE BINARY(1000000),
				    CONSTRAINT PK_PRODUCT PRIMARY KEY (ID)
				)""");
		dsl.execute("CREATE SEQUENCE SQ_PRODUCT START WITH 1 INCREMENT BY 1");
	}

	private void createTablePurchase() {
		dsl.execute("""
				CREATE TABLE EN_PURCHASE (
				    ID BIGINT NOT NULL,
				    USERID BIGINT NOT NULL,
				    BUYDATE TIMESTAMP NOT NULL,
				    CONSTRAINT PK_PURCHASE PRIMARY KEY (ID),
				    CONSTRAINT FK_PURCHASE_USER FOREIGN KEY (USERID) REFERENCES EN_USER(ID)
				)""");
		dsl.execute("CREATE SEQUENCE SQ_PURCHASE START WITH 1 INCREMENT BY 1");
	}

	private void createTablePurchaseItem() {
		dsl.execute("""
				CREATE TABLE EN_PURCHASEITEM (
				    ID BIGINT NOT NULL,
				    PURCHASEID BIGINT NOT NULL,
				    PRODUCTID BIGINT NOT NULL,
				    AMOUNT INT NOT NULL,
				    PRICE NUMERIC(20,2) NOT NULL,
				    CONSTRAINT PK_PURCHASEITEM PRIMARY KEY (ID),
				    CONSTRAINT FK_PURCHASEITEM_PRODUCT FOREIGN KEY (PRODUCTID) REFERENCES EN_PRODUCT(ID),
				    CONSTRAINT FK_PURCHASEITEM_PURCHASE FOREIGN KEY (PURCHASEID) REFERENCES EN_PURCHASE(ID)
				)""");
		dsl.execute("CREATE SEQUENCE SQ_PURCHASEITEM START WITH 1 INCREMENT BY 1");
	}
}
