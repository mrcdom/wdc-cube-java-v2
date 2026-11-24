package br.com.wdc.framework.domain.transaction;

/**
 * Handle of the current transaction, handed to the work executed by {@link TransactionService}.
 *
 * <p>
 * It does not expose the lifecycle: opening, committing and rolling back are the framework's responsibility (the
 * boundary is the lambda). The work uses this handle only to mark rollback and query the transaction state.
 * </p>
 */
public interface TransactionContext {

    /**
     * Marks the current transaction for rollback. A <b>one-way</b> operation: once marked, it cannot be unmarked, and the
     * framework rolls back at the end of the scope.
     *
     * @throws IllegalStateException if there is no active transaction (e.g. inside {@code never()} or
     *                               {@code notSupported()}).
     */
    void setRollbackOnly();

    /** @return {@code true} if the current transaction is already marked for rollback. */
    boolean isRollbackOnly();

    /** @return {@code true} if there is an active transaction in this scope (false in {@code never}/{@code notSupported},
     *          or in {@code supports} when there was no transaction on entry). */
    boolean isActive();

}
