package br.com.wdc.store.scripts.sgbd;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.security.PasswordVerifiers;
import br.com.wdc.framework.domain.security.ScramVerifier;
import br.com.wdc.framework.jooq.JsonDialect;
import br.com.wdc.store.scripts.codegen.GenerateJooqSchema;

/**
 * Creates the tables that are missing, using jOOQ, brings older databases up to the current schema, and seeds the
 * database ({@link DBReset}) when it created one or when asked to reset.
 *
 * <p>The DDL statements here are the single source of truth for the schema.
 * The jOOQ generated classes (in persistence.impl, package {@code scheme}) are derived from this via
 * {@link GenerateJooqSchema}.</p>
 */
public class DBCreate {

	private Connection connection;
	private DSLContext dsl;

	boolean mustResetDb = false;
	private boolean skipReset = false;

	public DBCreate withConnection(Connection connection) {
		this.connection = connection;
		this.dsl = DSL.using(connection, detectDialect(connection));
		return this;
	}

	/** Detects the jOOQ SQLDialect from the JDBC connection metadata. */
	private static SQLDialect detectDialect(Connection connection) {
		try {
			String url = connection.getMetaData().getURL();
			if (url != null) {
				if (url.startsWith("jdbc:postgresql:") || url.startsWith("jdbc:pgsql:")) {
					return SQLDialect.POSTGRES;
				}
			}
		} catch (Exception ignored) {
			// fall through to default
		}
		return SQLDialect.H2;
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
		JsonDialect.of(this.dsl.dialect()).initialize(this.connection);

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

		if (!existingTables.contains("EN_LOGIN_FAILURE")) {
			createTableLoginFailure();
		}

		if (!existingTables.contains("EN_USER_SESSION")) {
			createTableUserSession();
		}

		if (!existingTables.contains("EN_AUTH_NONCE")) {
			createTableAuthNonce();
		}

		upgradePasswordsToVerifiers();

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
				    PASSWORD VARCHAR(255) NOT NULL,
				    NAME VARCHAR(255) NOT NULL,
				    ROLES VARCHAR(255),
				    CONSTRAINT PK_USER PRIMARY KEY (ID)
				)""");
		dsl.execute("CREATE SEQUENCE SQ_USER START WITH 1 INCREMENT BY 1");
		// Support the NAME_A_TO_Z and LOGIN_A_TO_Z orderings of UserCriteria.
		dsl.execute("CREATE INDEX IX_USER_NAME ON EN_USER (NAME)");
		dsl.execute("CREATE INDEX IX_USER_USERNAME ON EN_USER (USERNAME)");
	}

	/** The failed sign-ins of each login ({@code LoginThrottle}): the count in a row and the last one, in UTC. */
	private void createTableLoginFailure() {
		dsl.execute("""
				CREATE TABLE EN_LOGIN_FAILURE (
				    USERNAME VARCHAR(100) NOT NULL,
				    FAILURES INT NOT NULL,
				    LAST_FAILURE TIMESTAMP NOT NULL,
				    CONSTRAINT PK_LOGIN_FAILURE PRIMARY KEY (USERNAME)
				)""");
		dsl.execute("CREATE INDEX IX_LOGIN_FAILURE_LAST ON EN_LOGIN_FAILURE (LAST_FAILURE)");
	}

	/**
	 * The users' sessions ({@code AccessContextCache}): who, with which roles, the refresh token, when the access token
	 * expires (UTC), and the session's RSA key pair — the private key encrypted with a key derived from the JWT secret.
	 */
	private void createTableUserSession() {
		dsl.execute("""
				CREATE TABLE EN_USER_SESSION (
				    SESSION_ID VARCHAR(36) NOT NULL,
				    USERID BIGINT NOT NULL,
				    USERNAME VARCHAR(100) NOT NULL,
				    ROLES VARCHAR(255),
				    REFRESH_TOKEN VARCHAR(36) NOT NULL,
				    EXPIRES_AT TIMESTAMP NOT NULL,
				    PUBLIC_KEY VARCHAR(1000) NOT NULL,
				    PRIVATE_KEY VARCHAR(4000) NOT NULL,
				    CONSTRAINT PK_USER_SESSION PRIMARY KEY (SESSION_ID),
				    CONSTRAINT UK_USER_SESSION_REFRESH UNIQUE (REFRESH_TOKEN)
				)""");
		dsl.execute("CREATE INDEX IX_USER_SESSION_EXPIRES ON EN_USER_SESSION (EXPIRES_AT)");
	}

	/** The nonces of the sign-in challenges in flight ({@code NonceStore}): for whom, and until when (UTC). */
	private void createTableAuthNonce() {
		dsl.execute("""
				CREATE TABLE EN_AUTH_NONCE (
				    NONCE VARCHAR(36) NOT NULL,
				    USERNAME VARCHAR(100) NOT NULL,
				    EXPIRES_AT TIMESTAMP NOT NULL,
				    CONSTRAINT PK_AUTH_NONCE PRIMARY KEY (NONCE)
				)""");
		dsl.execute("CREATE INDEX IX_AUTH_NONCE_EXPIRES ON EN_AUTH_NONCE (EXPIRES_AT)");
	}

	// -- Upgrades of older databases --

	/**
	 * A database from before the SCRAM sign-in stores each password as its MD5 digest (base 36) in a {@code CHAR(32)}:
	 * the column is widened to hold a verifier, and each digest becomes the verifier of the same password
	 * ({@link PasswordVerifiers#fromDigest}) — the conversion needs no password, and every user keeps signing in with
	 * the same one.
	 */
	private void upgradePasswordsToVerifiers() {
		if (passwordColumnTooNarrow()) {
			dsl.execute("ALTER TABLE EN_USER ALTER COLUMN PASSWORD SET DATA TYPE VARCHAR(255)");
		}
		var digests = dsl.fetch("SELECT ID, PASSWORD FROM EN_USER WHERE PASSWORD NOT LIKE ?", ScramVerifier.MECHANISM + "$%");
		for (var row : digests) {
			var stored = row.get(1, String.class);
			if (stored == null || stored.isBlank() || ScramVerifier.parse(stored) != null) {
				continue;
			}
			dsl.execute("UPDATE EN_USER SET PASSWORD = ? WHERE ID = ?", PasswordVerifiers.fromDigest(stored.trim()),
					row.get(0, Long.class));
		}
	}

	/** Whether {@code EN_USER.PASSWORD} is still the old fixed-width column, or narrower than a verifier needs. */
	private boolean passwordColumnTooNarrow() {
		var table = dsl.meta().getTables().stream()
				.filter(t -> "EN_USER".equalsIgnoreCase(t.getName()))
				.findFirst()
				.orElseThrow();
		Field<?> password = null;
		for (var field : table.fields()) {
			if ("PASSWORD".equalsIgnoreCase(field.getName())) {
				password = field;
			}
		}
		if (password == null) {
			throw new IllegalStateException("EN_USER has no PASSWORD column");
		}
		var type = password.getDataType();
		var typeName = type.getTypeName().toLowerCase(Locale.ROOT);
		boolean varying = typeName.contains("varying") || typeName.contains("varchar");
		return !varying || (type.hasLength() && type.length() < 255);
	}

	private void createTableProduct() {
		boolean isPostgres = dsl.dialect() == SQLDialect.POSTGRES;
		String nameType  = isPostgres ? "TEXT"  : "VARCHAR_IGNORECASE(1000000)";
		String imageType = isPostgres ? "BYTEA" : "VARBINARY(1000000)";
		String descType  = isPostgres ? "TEXT"  : "VARCHAR(1000000)";
		dsl.execute("""
				CREATE TABLE EN_PRODUCT (
				    ID BIGINT NOT NULL,
				    NAME %s NOT NULL,
				    PRICE NUMERIC(20,2) NOT NULL,
				    DESCRIPTION %s NOT NULL,
				    IMAGE %s,
				    CONSTRAINT PK_PRODUCT PRIMARY KEY (ID)
				)""".formatted(nameType, descType, imageType));
		dsl.execute("CREATE SEQUENCE SQ_PRODUCT START WITH 1 INCREMENT BY 1");
		// Support NAME_A_TO_Z, CHEAPEST_FIRST and MOST_EXPENSIVE_FIRST of ProductCriteria.
		dsl.execute("CREATE INDEX IX_PRODUCT_NAME ON EN_PRODUCT (NAME)");
		dsl.execute("CREATE INDEX IX_PRODUCT_PRICE ON EN_PRODUCT (PRICE)");
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
		// Supports MOST_RECENT_PURCHASE_FIRST and EARLIEST_PURCHASE_FIRST of PurchaseCriteria.
		dsl.execute("CREATE INDEX IX_PURCHASE_BUYDATE ON EN_PURCHASE (BUYDATE)");
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
		// Support MOST_EXPENSIVE_FIRST and LARGEST_QUANTITY_FIRST of PurchaseItemCriteria.
		dsl.execute("CREATE INDEX IX_PURCHASEITEM_PRICE ON EN_PURCHASEITEM (PRICE)");
		dsl.execute("CREATE INDEX IX_PURCHASEITEM_AMOUNT ON EN_PURCHASEITEM (AMOUNT)");
	}
}
