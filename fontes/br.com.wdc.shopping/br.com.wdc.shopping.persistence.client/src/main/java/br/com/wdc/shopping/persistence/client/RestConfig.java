package br.com.wdc.shopping.persistence.client;

/**
 * Configuração compartilhada para os REST repositories.
 * Encapsula {@link HttpTransport} e gerencia o {@link RestAuthClient}.
 */
public class RestConfig {

    private final HttpTransport transport;
    private volatile RestAuthClient authClient;

    public RestConfig(HttpTransport transport) {
        this.transport = transport;
    }

    public HttpTransport transport() {
        return transport;
    }

    public void setAuthClient(RestAuthClient authClient) {
        this.authClient = authClient;
        transport.setAccessTokenSupplier(authClient != null ? authClient::accessToken : null);
    }

    public RestAuthClient authClient() {
        return authClient;
    }

    public String postJson(String path, String body) {
        return transport.postJson(path, body);
    }

    public String postJsonNullable(String path, String body) {
        return transport.postJsonNullable(path, body);
    }

    public String postJsonPublic(String path, String body) {
        return transport.postJsonPublic(path, body);
    }

    public String postJsonWithAuth(String path, String body, String token) {
        return transport.postJsonWithAuth(path, body, token);
    }

    public String getJson(String path) {
        return transport.getJson(path);
    }

    public byte[] getBytes(String path) {
        return transport.getBytes(path);
    }

    public boolean putBytes(String path, byte[] data) {
        return transport.putBytes(path, data);
    }
}
