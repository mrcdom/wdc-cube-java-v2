package br.com.wdc.framework.domain.exception;

/**
 * The caller asked too often, or failed too often: it must wait before trying again. Mapped to <b>HTTP 429 (Too Many
 * Requests)</b>, with {@code Retry-After} when {@link #retryAfterSeconds()} is known; a REST client gets it back for
 * any 429.
 */
public class TooManyRequestsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Long retryAfterSeconds;

    public TooManyRequestsException(String message) {
        this(message, null);
    }

    public TooManyRequestsException(String message, Long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** The seconds to wait before trying again; {@code null} when not known. */
    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
