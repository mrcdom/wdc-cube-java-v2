package br.com.wdc.framework.commons.storage;


/**
 * Abstração de armazenamento chave-valor no cliente.
 * <p>
 * Cada plataforma fornece duas implementações por escopo, acessíveis via a
 * instância de aplicação:
 * <ul>
 *   <li>{@code app.clientSessionStore()} — in-memory, vive enquanto a instância de app estiver ativa.</li>
 *   <li>{@code app.clientPersistentStore()} — persiste entre reinicializações (backing plain).</li>
 * </ul>
 * Para armazenar valores sensíveis, use {@link #secure()} em qualquer dos escopos:
 * <pre>
 *   app.clientPersistentStore().secure().set("auth.token", token);
 *   app.clientSessionStore().secure().set("csrf.token", csrf);
 * </pre>
 */
public interface ClientStorage {

    /**
     * Retorna uma visão deste escopo que usa backing seguro (Keychain,
     * FlutterSecureStorage, etc.) para armazenar valores em repouso.
     * <p>
     * Implementações que não suportam backing seguro nativo (ex.: in-memory,
     * Preferences desktop) podem retornar {@code this}.
     */
    ClientStorage secure();

    /**
     * Retorna o valor associado à chave, ou {@code null} se não houver.
     */
    String get(String key);

    /**
     * Armazena um valor associado à chave.
     */
    void set(String key, String value);

    /**
     * Remove a entrada associada à chave.
     */
    void remove(String key);

}
