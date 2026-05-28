package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Funções SQL personalizadas para H2.
 *
 * <p>
 * Delega para {@link br.com.wdc.framework.jooq.dialect.H2JsonDialect#initialize(Connection)}.
 * Mantida para compatibilidade — prefira usar {@code JsonDialect.of(dialect).initialize(conn)}.
 * </p>
 *
 * <h2>Registro</h2>
 *
 * <pre>{@code
 * try (Connection conn = dataSource.getConnection()) {
 *     H2Functions.registerAll(conn);
 * }
 * }</pre>
 */
public final class H2Functions {

    private H2Functions() {
        // NOOP
    }

    /**
     * Registra todas as funções de compatibilidade no H2.
     *
     * @param conn conexão JDBC com o H2 (deve estar aberta)
     */
    public static void registerAll(Connection conn) throws SQLException {
        JsonDialect.of(org.jooq.SQLDialect.H2).initialize(conn);
    }
}
