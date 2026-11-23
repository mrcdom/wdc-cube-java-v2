package br.com.wdc.framework.domain.exception;

/**
 * Thrown when opening a remote transaction once a <b>cap</b> on open transactions is reached — global (protects the
 * connection pool) or per owner (keeps one client from monopolizing the pool). Mapped to <b>HTTP 429 (Too Many
 * Requests)</b>: the requester should finish pending transactions and try again.
 */
public class TransactionLimitExceededException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransactionLimitExceededException(String message) {
		super(message);
	}

}
