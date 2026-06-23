package br.com.wdc.framework.domain.exception;

/**
 * Lançada quando uma requisição conflita com o <b>estado transacional</b> do solicitante — não é negação de
 * autorização. Caso típico: uma escrita chega sem o cabeçalho de transação ({@code X-Tx-Id}) enquanto o solicitante
 * tem uma transação remota aberta; aceitá-la (autocommit) deixaria um registro órfão. Mapeada para <b>HTTP 409
 * (Conflict)</b>, distinta de {@link AccessDeniedException} (403), para não poluir o tratamento de erros de segurança.
 */
public class TransactionConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransactionConflictException(String message) {
		super(message);
	}

}
