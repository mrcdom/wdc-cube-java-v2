package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;

/**
 * Funções SQL personalizadas para compatibilidade H2 ↔ PostgreSQL.
 *
 * <p>
 * Registra apenas o alias {@code TO_JSON_BIN} para converter campos BINARY em strings Base64 (JSON). Todos os demais
 * tipos (números, strings, booleans, datas) são tratados via SQL inline portável em {@link JooqUtils}, sem necessidade
 * de funções customizadas.
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
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE ALIAS IF NOT EXISTS TO_JSON_BIN FOR "br.com.wdc.framework.jooq.H2Functions.toJsonBin"
                    """);
        }
    }

    /**
     * Converte um valor BINARY (byte[]) para string JSON Base64.
     *
     * <p>
     * Chamado pelo H2 como função SQL registrada via {@code CREATE ALIAS}. O H2 mapeia campos BINARY diretamente para
     * {@code byte[]} no Java, permitindo essa conversão sem problemas de tipo.
     * </p>
     *
     * @param value valor binário (pode ser null)
     * @return {@code "base64..."} com aspas JSON, ou null se value for null
     */
    public static String toJsonBin(byte[] value) {
        if (value == null) {
            return null;
        }
        return "\"" + Base64.getEncoder().encodeToString(value) + "\"";
    }
}
