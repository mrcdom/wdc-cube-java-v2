package br.com.wdc.framework.domain.transaction;

/**
 * Lançada quando uma transação ativa é proibida para a operação — em particular, por
 * {@link TransactionService#never(java.util.function.Consumer) never(...)} quando já existe transação no escopo.
 */
public class TransactionNotAllowedException extends TransactionException {

    private static final long serialVersionUID = -5102938476510293847L;

    public TransactionNotAllowedException(String message) {
        super(message);
    }

}
