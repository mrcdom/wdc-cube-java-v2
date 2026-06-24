package br.com.wdc.framework.domain.exception;

/**
 * Lançada ao abrir uma transação remota quando um <b>teto</b> de transações abertas é atingido — global (proteção do
 * pool de conexões) ou por dono (impede um cliente monopolizar o pool). Mapeada para <b>HTTP 429 (Too Many
 * Requests)</b>: o solicitante deve finalizar transações pendentes e tentar de novo.
 */
public class TransactionLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransactionLimitExceededException(String message) {
		super(message);
	}

}
