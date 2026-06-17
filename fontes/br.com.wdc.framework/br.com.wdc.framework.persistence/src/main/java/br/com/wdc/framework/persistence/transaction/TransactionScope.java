package br.com.wdc.framework.persistence.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * Frame de transação ligado à thread (via {@link ThreadLocal}), operando em dois modos:
 *
 * <ul>
 * <li><b>JTA</b> — quando {@link JtaTransactionManager#BEAN} está inicializado. Delega begin/commit/rollback ao
 * {@link TransactionManager} Narayana; a conexão JDBC é obtida do pool Agroal e enlistada como recurso XA.</li>
 * <li><b>JDBC</b> — quando {@link JtaTransactionManager#BEAN} é {@code null} (padrão). Gerencia a transação diretamente
 * via {@link Connection#commit()}/{@link Connection#rollback()}, sem TM externo.</li>
 * </ul>
 *
 * <p>
 * <b>Reentrância (propagação REQUIRED):</b> {@link #begin(DataSource)} junta-se à transação ativa quando há uma —
 * <b>compartilhando a mesma conexão do owner</b> (uma única transação física em ambos os modos) — ou abre uma nova e
 * torna-se owner. Apenas o owner comita/reverte e fecha a conexão no {@link #close()}; participantes são no-op.
 * </p>
 *
 * <p>
 * <b>Suspensão (REQUIRES_NEW / NOT_SUPPORTED):</b> {@link #suspend()} desliga a transação corrente da thread (JTA:
 * {@code tm.suspend()}; JDBC: a conexão/transação fica viva, sem dono na thread) e {@link #resume(TransactionScope)}
 * religa. No modo JDBC, o novo escopo abre uma <b>segunda conexão</b> do pool, independente da suspensa.
 * </p>
 *
 * <p>
 * Usado pelo TransactionService (estilo CMT) e pelos repositórios, que obtêm a conexão da transação corrente via
 * {@link #connection()}. Single-thread: não compartilhe entre threads.
 * </p>
 */
public final class TransactionScope implements AutoCloseable {

    private static final ThreadLocal<TransactionScope> CURRENT = new ThreadLocal<>();

    private final Connection connection;
    private final boolean owner;
    /** Não-nulo apenas no modo JDBC owner — {@code autoCommit} original a restaurar no {@link #close()}. */
    private final Boolean jdbcOldAutoCommit;
    /** Não-nulo apenas no modo JTA owner — transação Narayana, para suspend/resume. */
    private Transaction jtaTransaction;
    private boolean rollbackOnly;
    private boolean closed;

    private TransactionScope(Connection connection, boolean owner, Boolean jdbcOldAutoCommit) {
        this.connection = connection;
        this.owner = owner;
        this.jdbcOldAutoCommit = jdbcOldAutoCommit;
    }

    // :: Propagação REQUIRED

    /**
     * Junta-se à transação ativa (participante que compartilha a conexão do owner) ou abre uma nova (owner).
     */
    public static TransactionScope begin(DataSource dataSource) throws SQLException, SystemException {
        var current = CURRENT.get();
        if (current != null && !current.closed) {
            // Participante: compartilha a conexão (e portanto a transação) do owner. Sem ciclo de vida próprio.
            return new TransactionScope(current.connection, false, null);
        }
        return beginNew(dataSource);
    }

    /**
     * Abre incondicionalmente uma nova transação owner e a instala como corrente. Deve ser precedido de
     * {@link #suspend()} se já houver transação ativa (caso REQUIRES_NEW).
     */
    public static TransactionScope beginNew(DataSource dataSource) throws SQLException, SystemException {
        var tm = JtaTransactionManager.BEAN.get();
        return tm != null ? beginJta(dataSource, tm) : beginJdbc(dataSource);
    }

    private static TransactionScope beginJta(DataSource dataSource, TransactionManager tm)
            throws SQLException, SystemException {
        try {
            tm.begin();
        } catch (jakarta.transaction.NotSupportedException e) {
            throw new SystemException("Não foi possível iniciar a transação JTA: " + e.getMessage());
        }
        Connection connection;
        Transaction jtaTx;
        try {
            connection = dataSource.getConnection(); // Agroal enlista automaticamente em tm
            jtaTx = tm.getTransaction();
        } catch (SQLException | SystemException e) {
            try {
                tm.rollback();
            } catch (SystemException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        var ctx = new TransactionScope(connection, true, null);
        ctx.jtaTransaction = jtaTx;
        CURRENT.set(ctx);
        return ctx;
    }

    private static TransactionScope beginJdbc(DataSource dataSource) throws SQLException {
        var connection = dataSource.getConnection();
        var oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        var ctx = new TransactionScope(connection, true, oldAutoCommit);
        CURRENT.set(ctx);
        return ctx;
    }

    // :: Suspensão / Retomada (REQUIRES_NEW, NOT_SUPPORTED)

    /**
     * Desliga a transação corrente da thread e a retorna para posterior {@link #resume(TransactionScope)}. Retorna
     * {@code null} se não houver transação ativa. A transação suspensa permanece viva (não é comitada).
     */
    public static TransactionScope suspend() {
        var current = CURRENT.get();
        if (current == null || current.closed) {
            return null;
        }
        CURRENT.remove();
        var tm = JtaTransactionManager.BEAN.get();
        if (tm != null) {
            try {
                tm.suspend(); // desenlista a tx JTA da thread; current.jtaTransaction guarda a referência
            } catch (SystemException e) {
                throw new IllegalStateException("Falha ao suspender transação JTA", e);
            }
        }
        return current;
    }

    /**
     * Religa uma transação previamente suspensa por {@link #suspend()}. No-op se {@code suspended} for {@code null}.
     */
    public static void resume(TransactionScope suspended) {
        if (suspended == null) {
            return;
        }
        var tm = JtaTransactionManager.BEAN.get();
        if (tm != null) {
            try {
                tm.resume(suspended.jtaTransaction);
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao retomar transação JTA", e);
            }
        }
        CURRENT.set(suspended);
    }

    // :: Acesso / estado

    /** Contexto corrente da thread, ou {@code null} se não houver transação ativa. */
    public static TransactionScope current() {
        var current = CURRENT.get();
        return (current != null && !current.closed) ? current : null;
    }

    /** Verifica se há uma transação ativa na thread atual. */
    public static boolean isActive() {
        return current() != null;
    }

    /**
     * Obtém a conexão da transação corrente.
     *
     * @throws IllegalStateException se o contexto já foi fechado
     */
    public Connection connection() {
        if (closed) {
            throw new IllegalStateException("TransactionScope already closed");
        }
        return connection;
    }

    public boolean isRollbackOnly() {
        if (owner) {
            return rollbackOnly;
        }
        var cur = current();
        return cur != null && cur.rollbackOnly;
    }

    /** Marca a transação corrente para rollback (one-way). O commit não será executado no close do owner. */
    public void setRollbackOnly() {
        if (owner) {
            this.rollbackOnly = true;
        } else {
            var current = CURRENT.get();
            if (current != null) {
                current.rollbackOnly = true;
            }
        }
    }

    // :: Encerramento

    /**
     * Encerra o frame. Participante: no-op (a conexão pertence ao owner). Owner: comita (ou faz rollback se marcado) e
     * fecha a conexão.
     */
    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;

        if (!owner) {
            // Participante: compartilha a conexão do owner — nada a fechar/finalizar.
            return;
        }

        CURRENT.remove();

        if (jdbcOldAutoCommit == null) {
            // JTA: devolve conexão ao pool; TM comita/reverte via Synchronization.
            silentClose(connection);
            commitOrRollbackJta();
        } else {
            // JDBC: comita/reverte via conexão e fecha.
            commitOrRollbackJdbc();
        }
    }

    // -------------------------------------------------------------------------

    private void commitOrRollbackJta() throws SQLException {
        var tm = JtaTransactionManager.get();
        try {
            if (rollbackOnly || isMarkedRollback(tm)) {
                tm.rollback();
            } else {
                tm.commit();
            }
        } catch (Exception e) {
            throw new SQLException("Falha ao finalizar transação JTA", e);
        }
    }

    private void commitOrRollbackJdbc() throws SQLException {
        try {
            if (rollbackOnly) {
                connection.rollback();
            } else {
                connection.commit();
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(jdbcOldAutoCommit);
            } catch (SQLException _ignore) {
                // melhor esforço
            }
            silentClose(connection);
        }
    }

    private static void silentClose(Connection connection) {
        try {
            connection.close();
        } catch (Exception __ignore) {
            // melhor esforço
        }
    }

    private static boolean isMarkedRollback(TransactionManager tm) {
        try {
            return tm.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException _ignore) {
            return false;
        }
    }
}
