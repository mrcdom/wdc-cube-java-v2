package br.com.wdc.framework.domain.transaction;

/**
 * Exceção base (unchecked) do controle transacional do {@link TransactionService}.
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
