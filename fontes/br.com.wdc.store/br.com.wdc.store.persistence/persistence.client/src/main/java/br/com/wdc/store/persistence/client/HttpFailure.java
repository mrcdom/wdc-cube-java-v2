package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TransactionLimitExceededException;

/**
 * An API error response as the exception the server threw: the inverse of the {@code RepositoryApiRoutes} mapping
 * (400, 403, 409 and 429, with {@code {"error": "..."}} in the body). That way whoever calls the remote repository
 * gets the same exception it would get from the local repository.
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
            case 429 -> new TransactionLimitExceededException(message);
            default -> new BusinessException("HTTP " + status + (body != null && !body.isBlank() ? ": " + body : ""));
        };
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
