package br.com.wdc.framework.commons.storage;


/**
 * Abstração de armazenamento chave-valor no cliente.
 * <p>
 * Cada plataforma/contexto fornece duas implementações por escopo:
 * <ul>
 *   <li>{@code app.clientSessionStore()} — in-memory, vive enquanto a instância de app
 *       (ou conexão, no caso do {@code HostClient}) estiver ativa.</li>
 *   <li>{@code app.clientPersistentStore()} — persiste entre reinicializações.
 *       No desktop/mobile usa {@link PreferencesClientStorage};
 *       no probe/bridge usa {@link InMemoryClientStorage} por default.</li>
 * </ul>
 * Para armazenar valores sensíveis, use {@link #secure()} em qualquer dos escopos:
 * <pre>
 *   app.clientPersistentStore().secure().set("auth.token", token);
 *   app.clientSessionStore().secure().set("csrf.token", csrf);
 * </pre>
 * <p>
 * O comportamento de {@link #secure()} depende da implementação:
 * <ul>
 *   <li>{@link PreferencesClientStorage} — retorna {@link EncryptedPreferencesClientStorage}
 *       (AES-256-GCM, chave gerada por instalação).</li>
 *   <li>{@link InMemoryClientStorage} — retorna {@code this}; dados efêmeros, criptografia
 *       não agrega valor.</li>
 * </ul>
 */
public interface ClientStorage {

    /**
     * Retorna uma visão deste escopo com backing seguro para os valores em repouso.
     * <p>
     * {@link PreferencesClientStorage} retorna {@link EncryptedPreferencesClientStorage}
     * (AES-256-GCM). {@link InMemoryClientStorage} retorna {@code this} — dados
     * são efêmeros e a criptografia não agrega valor nesse contexto.
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

    /**
     * Retorna todas as entradas deste escopo candidatas à sincronização.
     * Por convenção, apenas chaves prefixadas com {@code ~} são incluídas.
     * <p>
     * Implementações que não suportam sincronização (ex.: in-memory, desktop)
     * podem retornar o mapa padrão vazio.
     */
    default java.util.Map<String, String> all() {
        return java.util.Collections.emptyMap();
    }

}
