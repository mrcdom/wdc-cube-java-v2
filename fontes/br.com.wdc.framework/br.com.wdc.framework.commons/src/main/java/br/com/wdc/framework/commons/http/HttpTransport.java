package br.com.wdc.framework.commons.http;

import java.util.function.Supplier;

/**
 * Generic HTTP transport abstraction.
 * <p>
 * Works with JSON as String and raw bytes,
 * compatible with the JVM (OkHttp) and TeaVM (XMLHttpRequest).
 */
public interface HttpTransport {

	/**
	 * Sets the access token supplier for authenticated requests.
	 * Implementations must include the token as the header "Authorization: Bearer {token}".
	 */
	default void setAccessTokenSupplier(Supplier<String> tokenSupplier) {
		// default no-op for transports that handle auth some other way
	}

	/**
	 * Sets the remote transaction id supplier. When it returns non-null, the implementation must include the header
	 * {@code X-Tx-Id: {txId}} in the requests, propagating the client-driven transaction to the server.
	 */
	default void setTransactionIdSupplier(Supplier<String> txIdSupplier) {
		// default no-op for transports that do not support remote transactions
	}

	/**
	 * POST JSON with authentication (if available).
	 *
	 * @param path relative path (e.g. "/api/products/fetch")
	 * @param body JSON request body (String)
	 * @return JSON response (String)
	 */
	String postJson(String path, String body);

	/**
	 * POST JSON with authentication, returning null for 404.
	 */
	String postJsonNullable(String path, String body);

	/**
	 * POST JSON without authentication (public endpoints).
	 */
	String postJsonPublic(String path, String body);

	/**
	 * POST JSON with an explicit authentication token.
	 */
	String postJsonWithAuth(String path, String body, String token);

	/**
	 * GET JSON without authentication.
	 */
	String getJson(String path);

	/**
	 * GET bytes with authentication. Returns null for 404/204.
	 */
	byte[] getBytes(String path);

	/**
	 * PUT bytes with authentication.
	 *
	 * @return true on success
	 */
	boolean putBytes(String path, byte[] data);

}
