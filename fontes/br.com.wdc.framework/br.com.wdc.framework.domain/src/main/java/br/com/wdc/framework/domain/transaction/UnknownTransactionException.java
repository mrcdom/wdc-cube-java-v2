package br.com.wdc.framework.domain.transaction;

/**
 * A remote transaction this server does not know: never begun here, already ended or reaped, or held by another
 * instance of the server. The REST API answers it with 410.
 */
public class UnknownTransactionException extends TransactionSystemException {

    private static final long serialVersionUID = 2793115630586114209L;

    public UnknownTransactionException(String message) {
        super(message);
    }
}
