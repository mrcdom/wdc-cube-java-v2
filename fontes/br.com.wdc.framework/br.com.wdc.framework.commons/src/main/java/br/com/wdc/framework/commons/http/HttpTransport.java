package br.com.wdc.framework.commons.http;

import java.util.function.Supplier;

/**
 * Abstração de transporte HTTP genérica.
 * <p>
 * Trabalha com JSON em formato String e bytes crus,
 * compatível com JVM (OkHttp) e TeaVM (XMLHttpRequest).
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
	 * @param body corpo JSON da requisição (String)
	 * @return resposta JSON (String)
	 */
	String postJson(String path, String body);

	/**
	 * POST JSON com autenticação, retornando null para 404.
	 */
	String postJsonNullable(String path, String body);

	/**
	 * POST JSON sem autenticação (endpoints públicos).
	 */
	String postJsonPublic(String path, String body);

	/**
	 * POST JSON com token de autenticação explícito.
	 */
	String postJsonWithAuth(String path, String body, String token);

	/**
	 * GET JSON sem autenticação.
	 */
	String getJson(String path);

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
