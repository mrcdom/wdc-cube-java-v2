package br.com.wdc.shopping.persistence.client;

import java.util.function.Supplier;

import com.google.gson.JsonObject;

/**
 * Abstração de transporte HTTP para os REST repositories.
 * <p>
 * Permite substituir a implementação HTTP (OkHttp, fetch API do browser, etc.)
 * sem alterar a lógica dos repositórios REST.
 */
public interface HttpTransport {

    /**
     * Define o fornecedor de token de acesso para requisições autenticadas.
     * Implementações devem incluir o token como header "Authorization: Bearer {token}".
     */
    default void setAccessTokenSupplier(Supplier<String> tokenSupplier) {
        // default no-op para transports que gerenciam auth de outra forma
    }

    /**
     * POST JSON com autenticação (se disponível).
     *
     * @param path caminho relativo (e.g. "/api/products/fetch")
     * @param body corpo JSON da requisição
     * @return resposta JSON
     * @throws br.com.wdc.shopping.domain.exception.BusinessException em caso de erro HTTP
     */
    JsonObject postJson(String path, JsonObject body);

    /**
     * POST JSON com autenticação, retornando null para 404.
     */
    JsonObject postJsonNullable(String path, JsonObject body);

    /**
     * POST JSON sem autenticação (endpoints públicos).
     */
    JsonObject postJsonPublic(String path, JsonObject body);

    /**
     * POST JSON com token de autenticação explícito.
     */
    JsonObject postJsonWithAuth(String path, JsonObject body, String token);

    /**
     * GET JSON sem autenticação.
     */
    JsonObject getJson(String path);

    /**
     * GET bytes com autenticação. Retorna null para 404/204.
     */
    byte[] getBytes(String path);

    /**
     * PUT bytes com autenticação.
     *
     * @return true se sucesso
     */
    boolean putBytes(String path, byte[] data);

}
