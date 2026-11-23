package br.com.wdc.framework.domain.exception;

/**
 * Thrown when a request conflicts with the requester's <b>transactional state</b> — it is not an authorization
 * denial. Typical case: a write arrives without the transaction header ({@code X-Tx-Id}) while the requester has an
 * open remote transaction; accepting it (autocommit) would leave an orphan record. Mapped to <b>HTTP 409
 * (Conflict)</b>, distinct from {@link AccessDeniedException} (403), so as not to pollute security error handling.
 */
public class TransactionConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TransactionConflictException(String message) {
		super(message);
	}

}
