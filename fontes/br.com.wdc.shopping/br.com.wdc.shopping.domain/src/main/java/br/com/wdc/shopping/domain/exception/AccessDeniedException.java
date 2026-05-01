package br.com.wdc.shopping.domain.exception;

/**
 * Lançada quando uma operação é negada por falta de permissão ou autenticação.
 */
public class AccessDeniedException extends RuntimeException {

	public AccessDeniedException(String message) {
		super(message);
	}

}
