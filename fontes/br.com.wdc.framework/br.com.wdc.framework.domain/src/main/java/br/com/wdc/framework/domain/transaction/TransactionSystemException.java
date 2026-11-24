package br.com.wdc.framework.domain.transaction;

/**
 * Thrown when a transaction infrastructure operation fails in a way that affects its health — for example, a failure
 * in commit or rollback, suspend/resume (REQUIRES_NEW / NOT_SUPPORTED) or obtaining a connection. Signals that the
 * consistency of the transaction could not be guaranteed.
 */
public class TransactionSystemException extends TransactionException {

    private static final long serialVersionUID = -8471920384756102938L;

    public TransactionSystemException(String message) {
        super(message);
    }

    public TransactionSystemException(String message, Throwable cause) {
        super(message, cause);
    }

}
