package br.com.wdc.framework.commons.util;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Gerenciador de transação local com propagação REQUIRED via {@link ThreadLocal}.
 *
 * <p>
 * Uso típico:
 * </p>
 * 
 * <pre>{@code
 * try (var tx = TransactionContext.begin(dataSource)) {
 *     // operações usando tx.connection()
 * }
 * // commit automático ao fechar; rollback em caso de exceção
 * </pre>

 *
 * 
<p>
Composição transacional (mesma conexão/transação):
</p>
 * 
 * 
 * <pre>{@code
 * try (var tx = TransactionContext.begin(dataSource)) {
 *     shoppingService.purchase(userId, items);    // participa da tx existente
 *     shoppingService.loadPurchases(userId);      // mesma conexão
 * }
 * </pre>
 */
public final class TransactionContext implements AutoCloseable {

	private static final ThreadLocal<TransactionContext> CURRENT = new ThreadLocal<>();

	private final Connection connection;
	private final boolean owner;
	private final boolean oldAutoCommit;
	private boolean rollbackOnly;
	private boolean closed;

	private TransactionContext(Connection connection, boolean owner, boolean oldAutoCommit) {
		this.connection = connection;
		this.owner = owner;
		this.oldAutoCommit = oldAutoCommit;
	}

	/**
	 * Inicia ou participa de uma transação (propagação REQUIRED).
	 * <ul>
	 * <li>Se não há transação ativa: abre conexão, desabilita autoCommit, torna-se owner.</li>
	 * <li>Se já há transação ativa: retorna contexto participante (mesma conexão, sem gerência de ciclo de vida).</li>
	 * </ul>
	 */
	public static TransactionContext begin(DataSource dataSource) throws SQLException {
		var current = CURRENT.get();
		if (current != null && !current.closed) {
			return new TransactionContext(current.connection, false, current.oldAutoCommit);
		}

		var connection = dataSource.getConnection();
		var oldAutoCommit = connection.getAutoCommit();
		connection.setAutoCommit(false);

		var ctx = new TransactionContext(connection, true, oldAutoCommit);
		CURRENT.set(ctx);
		return ctx;
	}

	/**
	 * Obtém a conexão da transação corrente.
	 *
	 * @throws IllegalStateException se o contexto já foi fechado
	 */
	public Connection connection() {
		if (closed) {
			throw new IllegalStateException("TransactionContext already closed");
		}
		return connection;
	}

	/**
	 * Verifica se há uma transação ativa na thread atual.
	 */
	public static boolean isActive() {
		var current = CURRENT.get();
		return current != null && !current.closed;
	}

	/**
	 * Marca a transação para rollback. O commit não será executado no close do owner.
	 */
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

	/**
	 * Fecha o contexto:
	 * <ul>
	 * <li>Owner: commit ou rollback, fecha conexão e limpa ThreadLocal.</li>
	 * <li>Participante: noop.</li>
	 * </ul>
	 */
	@Override
	public void close() throws SQLException {
		if (closed) {
			return;
		}
		closed = true;

		if (!owner) {
			return;
		}

		CURRENT.remove();

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
			connection.setAutoCommit(this.oldAutoCommit);
			connection.close();
		}
	}
}
