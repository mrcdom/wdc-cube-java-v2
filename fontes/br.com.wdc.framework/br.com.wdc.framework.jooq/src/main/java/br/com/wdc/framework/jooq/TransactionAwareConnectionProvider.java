package br.com.wdc.framework.jooq;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

import br.com.wdc.framework.persistence.transaction.TransactionScope;

/**
 * jOOQ {@link ConnectionProvider} aware of the {@link TransactionScope} — binds the shared {@code DSLContext} to the
 * programmatic transaction control ({@code TransactionService}).
 *
 * <ul>
 * <li><b>Inside a transaction</b> ({@link TransactionScope#current()} != {@code null}): hands out the connection of the
 * current scope, so that all queries of the block share the <b>same physical transaction</b>. It does not close the
 * connection in {@link #release(Connection)} — the {@code TransactionScope} owns it and finalizes it when the scope ends.</li>
 * <li><b>Outside a transaction</b>: borrows a standalone connection from the {@link DataSource} (autocommit) and returns it
 * to the pool in {@link #release(Connection)}.</li>
 * </ul>
 *
 * <p>
 * Works in both modes supported by the {@code TransactionScope}: JDBC (the scope's single connection) and JTA (connection
 * enlisted in the TransactionManager). Single-threaded, like the scope itself.
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
            throw new DataAccessException("Could not obtain a connection from the DataSource", e);
        }
    }

    @Override
    public void release(Connection connection) {
        // In a transaction, the connection belongs to the TransactionScope (which finalizes it when the scope
        // closes) — do not close it here.
        // acquire()/release() bracket a single statement execution on the same thread, so the state is stable.
        if (TransactionScope.current() != null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to release standalone connection", e);
        }
    }
}
