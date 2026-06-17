package br.com.wdc.framework.domain.transaction;

/**
 * Lançada quando uma operação exige transação ativa e não há nenhuma — em particular, por
 * {@link TransactionService#mandatory(java.util.function.Consumer) mandatory(...)}.
 */
public class TransactionRequiredException extends TransactionException {

    private static final long serialVersionUID = -2918374650192837465L;

    public TransactionRequiredException(String message) {
        super(message);
    }

}
