package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

import br.com.wdc.framework.persistence.transaction.TransactionScope;

/**
 * {@link ConnectionProvider} do jOOQ ciente do {@link TransactionScope} — liga o {@code DSLContext} compartilhado ao
 * controle de transação programático ({@code TransactionService}).
 *
 * <ul>
 * <li><b>Dentro de uma transação</b> ({@link TransactionScope#current()} != {@code null}): entrega a conexão do escopo
 * corrente, de modo que todas as queries do bloco compartilhem a <b>mesma transação física</b>. Não fecha a conexão no
 * {@link #release(Connection)} — o {@code TransactionScope} é o dono e a finaliza ao encerrar o escopo.</li>
 * <li><b>Fora de transação</b>: empresta uma conexão avulsa do {@link DataSource} (autocommit) e a devolve ao pool no
 * {@link #release(Connection)}.</li>
 * </ul>
 *
 * <p>
 * Funciona nos dois modos suportados pelo {@code TransactionScope}: JDBC (conexão única do escopo) e JTA (conexão
 * enlistada no TransactionManager). Single-thread, como o próprio escopo.
 * </p>
 */
public final class TransactionAwareConnectionProvider implements ConnectionProvider {

    private final DataSource dataSource;

    public TransactionAwareConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection acquire() {
        var scope = TransactionScope.current();
        if (scope != null) {
            return scope.connection();
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("Não foi possível obter conexão do DataSource", e);
        }
    }

    @Override
    public void release(Connection connection) {
        // Em transação, a conexão pertence ao TransactionScope (que a finaliza no close do escopo) — não fechar aqui.
        // acquire()/release() bracketeiam uma única execução de statement na mesma thread, então o estado é estável.
        if (TransactionScope.current() != null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Falha ao liberar conexão avulsa", e);
        }
    }
}
