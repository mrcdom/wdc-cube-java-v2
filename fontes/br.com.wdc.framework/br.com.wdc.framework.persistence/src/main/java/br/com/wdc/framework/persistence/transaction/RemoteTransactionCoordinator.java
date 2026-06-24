package br.com.wdc.framework.persistence.transaction;

/**
 * Coordenador de transações <b>dirigidas pelo cliente sobre REST</b> (lado servidor).
 *
 * <p>
 * Permite que o cliente demarque uma transação que atravessa <b>várias requisições HTTP</b> contra o mesmo backend: o
 * servidor mantém a transação física aberta entre as chamadas, identificada por um {@code txId}, e a religa
 * (resume) à thread que atende cada requisição.
 * </p>
 *
 * <ul>
 * <li>{@link #begin(String)} — abre uma transação, suspende-a e devolve seu {@code txId};</li>
 * <li>{@link #resume(String, String)} / {@link #suspend(String)} — religa/desliga a transação na thread corrente (em
 * torno de cada requisição que carrega o {@code txId});</li>
 * <li>{@link #commit(String, String)} / {@link #rollback(String, String)} — finalizam e removem do registro.</li>
 * </ul>
 *
 * <p>
 * <b>Dono (ownerKey):</b> uma chave <b>opaca</b> informada no {@link #begin(String)} e revalidada em todas as
 * operações seguintes — o coordenador apenas compara strings, sem conhecer o conceito de usuário. Se o {@code begin}
 * recebeu uma chave não-nula, qualquer operação com chave divergente é rejeitada. Quem traduz identidade → chave (e
 * o que fazer com {@code null}) é a camada chamadora.
 * </p>
 *
 * <p>
 * SPI <b>puramente server-side de persistência</b>: implementado neste módulo (ligado ao {@code DataSource} do
 * módulo) e consumido apenas pelo host REST. Não é visível à camada de apresentação — por isso vive em
 * {@code framework.persistence}, não em {@code framework.domain}. Cada {@code txId} mantém uma conexão/transação
 * viva até commit/rollback — por isso há timeout de limpeza para transações abandonadas.
 * </p>
 */
public interface RemoteTransactionCoordinator {

    /**
     * Abre uma transação (suspensa) e devolve seu identificador opaco.
     *
     * @param ownerKey chave opaca do dono (revalidada nas operações seguintes); {@code null} = sem dono
     */
    String begin(String ownerKey);

    /** Religa a transação à thread corrente. Falha se desconhecida, já em uso, ou se {@code ownerKey} divergir. */
    void resume(String txId, String ownerKey);

    /** Desliga a transação da thread corrente, mantendo-a viva para a próxima requisição. */
    void suspend(String txId);

    /**
     * Finaliza com COMMIT e remove do registro (valida {@code ownerKey}). <b>Idempotente</b>: repetir o commit de uma
     * transação já comitada (resposta anterior perdida) é um no-op de sucesso, dentro da janela de retenção do
     * desfecho. Repetir como rollback algo já comitado conflita ({@code TransactionConflictException}).
     */
    void commit(String txId, String ownerKey);

    /**
     * Finaliza com ROLLBACK e remove do registro (valida {@code ownerKey}). <b>Idempotente</b>, simétrico ao
     * {@link #commit(String, String)}.
     */
    void rollback(String txId, String ownerKey);

    /**
     * Estado da transação para o solicitante, desambiguando uma resposta de finalização perdida.
     *
     * @return {@code "open"} (registrada e viva), {@code "committed"}/{@code "rolledback"} (finalizada, dentro da
     *         retenção) ou {@code "unknown"} (desconhecida — nunca existiu ou retenção expirada). Valida {@code ownerKey}.
     */
    String status(String txId, String ownerKey);

    /** @return {@code true} se há transação registrada para {@code txId}. */
    boolean exists(String txId);

    /** @return snapshot de métricas (gauges atuais + contadores acumulados) para observabilidade. */
    RemoteTransactionStats stats();

    /**
     * @return {@code true} se há alguma transação remota aberta para este {@code ownerKey} (não-nulo). Permite ao host
     *         detectar uma escrita que chegou <b>sem</b> o {@code txId} embora o solicitante tenha transação remota em
     *         aberto — sinal de propagação quebrada do cabeçalho, que de outro modo autocommitaria fora da transação.
     */
    boolean hasOpenTransactionForOwner(String ownerKey);

}
