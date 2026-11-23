package br.com.wdc.shopping.persistence.client;

import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TransactionLimitExceededException;

/**
 * Uma resposta de erro da API como a exceção que o servidor lançou: o inverso do mapeamento de
 * {@code RepositoryApiRoutes} (400, 403, 409 e 429, com {@code {"error": "..."}} no corpo). Assim quem chama o
 * repositório remoto recebe a mesma exceção que receberia do repositório local.
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

    /** O {@code error} do corpo JSON; o corpo inteiro quando não é esse JSON. */
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
