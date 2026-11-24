package br.com.wdc.framework.domain.transaction;

/**
 * Thrown when an active transaction is forbidden for the operation — in particular, by
 * {@link TransactionService#never(java.util.function.Consumer) never(...)} when a transaction already exists in scope.
 */
public class TransactionNotAllowedException extends TransactionException {

    private static final long serialVersionUID = -5102938476510293847L;

    public TransactionNotAllowedException(String message) {
        super(message);
    }

}
