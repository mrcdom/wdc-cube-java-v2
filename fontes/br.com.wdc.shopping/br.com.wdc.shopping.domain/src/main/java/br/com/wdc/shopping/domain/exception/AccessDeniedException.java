package br.com.wdc.shopping.domain.exception;

/**
 * Lançada quando uma operação é negada por falta de permissão ou autenticação.
 */
public class AccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = -3885024981786952019L;

    public AccessDeniedException(String message) {
		super(message);
	}

}
