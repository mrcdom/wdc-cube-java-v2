package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Locale;

import org.junit.Test;

import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.domain.security.PasswordUtil;
import br.com.wdc.framework.domain.security.Scram;
import br.com.wdc.framework.domain.security.ScramVerifier;
import br.com.wdc.store.scripts.sgbd.DBCreate;

/**
 * A database from before the SCRAM sign-in — {@code EN_USER.PASSWORD CHAR(32)} with MD5 digests — comes up with each
 * digest turned into the verifier of the same password, and the column wide enough for it.
 */
public class PasswordMigrationTest {

    private final JceCryptoProvider crypto = new JceCryptoProvider();

    @Test
    public void digestsBecomeVerifiers_ofTheSamePasswords() throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:password-migration;DB_CLOSE_DELAY=-1", "sa", "sa")) {
            try (var stmt = connection.createStatement()) {
                stmt.execute("""
                        CREATE TABLE EN_USER (
                            ID BIGINT NOT NULL,
                            USERNAME VARCHAR(255) NOT NULL,
                            PASSWORD CHAR(32) NOT NULL,
                            NAME VARCHAR(255) NOT NULL,
                            ROLES VARCHAR(255),
                            CONSTRAINT PK_USER PRIMARY KEY (ID)
                        )""");
                stmt.execute("CREATE SEQUENCE SQ_USER START WITH 3 INCREMENT BY 1");
                stmt.execute("INSERT INTO EN_USER VALUES (1, 'jdoe', '" + PasswordUtil.hashPassword("jdoe", crypto)
                        + "', 'John Doe', 'CUSTOMER')");
                stmt.execute("INSERT INTO EN_USER VALUES (2, 'jroe', '" + PasswordUtil.hashPassword("s3cret", crypto)
                        + "', 'Jane Roe', 'CUSTOMER')");
            }

            new DBCreate()
                    .withConnection(connection)
                    .withSkipReset()
                    .run();

            assertEquals("CHARACTER VARYING", passwordColumnType(connection));
            var scram = new Scram(crypto);
            var jdoe = ScramVerifier.parse(passwordOf(connection, 1));
            var jroe = ScramVerifier.parse(passwordOf(connection, 2));
            assertNotNull(jdoe);
            assertNotNull(jroe);
            assertTrue(scram.matches(jdoe, "jdoe"));
            assertTrue(scram.matches(jroe, "s3cret"));

            // a second start leaves the verifiers as they are
            var before = passwordOf(connection, 1);
            new DBCreate()
                    .withConnection(connection)
                    .withSkipReset()
                    .run();
            assertEquals(before, passwordOf(connection, 1));
        }
    }

    private static String passwordOf(Connection connection, long id) throws SQLException {
        try (var ps = connection.prepareStatement("SELECT PASSWORD FROM EN_USER WHERE ID = ?")) {
            ps.setLong(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    private static String passwordColumnType(Connection connection) throws SQLException {
        try (var rs = connection.getMetaData().getColumns(null, null, "EN_USER", "PASSWORD")) {
            rs.next();
            return rs.getString("TYPE_NAME").toUpperCase(Locale.ROOT);
        }
    }
}
