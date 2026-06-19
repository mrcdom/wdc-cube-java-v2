package br.com.wdc.framework.domain.transaction;

/**
 * Lançada quando uma operação de infraestrutura da transação falha de forma que afeta sua saúde — por exemplo, falha no
 * commit ou no rollback, suspensão/retomada (REQUIRES_NEW / NOT_SUPPORTED) ou obtenção de conexão. Sinaliza que a
 * consistência da transação não pôde ser garantida.
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
