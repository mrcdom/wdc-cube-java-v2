package br.com.wdc.framework.domain.exception;

/**
 * Thrown when an operation is denied for lack of permission or authentication.
 */
public class AccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = -3885024981786952019L;

    public AccessDeniedException(String message) {
		super(message);
	}

}
