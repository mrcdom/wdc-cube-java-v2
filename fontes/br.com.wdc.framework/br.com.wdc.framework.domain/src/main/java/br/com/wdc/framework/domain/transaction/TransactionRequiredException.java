package br.com.wdc.framework.domain.transaction;

/**
 * Thrown when an operation requires an active transaction and there is none — in particular, by
 * {@link TransactionService#mandatory(java.util.function.Consumer) mandatory(...)}.
 */
public class TransactionRequiredException extends TransactionException {

    private static final long serialVersionUID = -2918374650192837465L;

    public TransactionRequiredException(String message) {
        super(message);
    }

}
