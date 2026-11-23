package br.com.wdc.framework.domain.transaction;

/**
 * Base (unchecked) exception of the {@link TransactionService} transaction control.
 */
public class TransactionException extends RuntimeException {

    private static final long serialVersionUID = -7234981056472830194L;

    public TransactionException(String message) {
        super(message);
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }

}
