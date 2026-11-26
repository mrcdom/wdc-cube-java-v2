package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;

/**
 * An API error response as the exception the server threw: the inverse of the {@code RepositoryApiRoutes} mapping
 * (400, 403, 409 and 429, with {@code {"error": "..."}} in the body, and the wait of a 429 in {@code retryAfter}). That
 * way whoever calls the remote repository gets the same exception it would get from the local repository.
 */
public final class HttpFailure {

    private HttpFailure() {
        // NOOP
    }

    public static RuntimeException of(int status, String body) {
        var message = errorMessageOf(body);
        return switch (status) {
            case 400 -> new InvalidRequestException(message);
            case 403 -> new AccessDeniedException(message);
            case 409 -> new TransactionConflictException(message);
            case 429 -> new TooManyRequestsException(message, retryAfterOf(body));
            default -> new BusinessException("HTTP " + status + (body != null && !body.isBlank() ? ": " + body : ""));
        };
    }

    /** The {@code retryAfter} of the JSON body (the seconds a 429 asks to wait); {@code null} when absent. */
    private static Long retryAfterOf(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            var reader = new JsonStreamReader(body);
            reader.beginObject();
            while (reader.hasNext()) {
                if ("retryAfter".equals(reader.nextName())) {
                    return InputCoerceUtils.asLong(reader);
                }
                reader.skipValue();
            }
        } catch (RuntimeException notTheErrorJson) {
            // no wait known
        }
        return null;
    }

    /** The {@code error} of the JSON body; the whole body when it is not that JSON. */
    private static String errorMessageOf(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            var reader = new JsonStreamReader(body);
            reader.beginObject();
            while (reader.hasNext()) {
                if ("error".equals(reader.nextName())) {
                    return reader.nextString();
                }
                reader.skipValue();
            }
        } catch (RuntimeException notTheErrorJson) {
            // falls through: the raw body is the best clue
        }
        return body;
    }
}
