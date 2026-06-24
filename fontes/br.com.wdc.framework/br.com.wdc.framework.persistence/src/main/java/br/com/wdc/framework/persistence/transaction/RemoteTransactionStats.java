package br.com.wdc.framework.persistence.transaction;

/**
 * Snapshot de observabilidade do {@link RemoteTransactionCoordinator}: <b>gauges</b> instantâneos (estado atual) e
 * <b>contadores</b> acumulados (totais desde o início). Imutável — adequado para expor num endpoint de métricas.
 *
 * @param openNow         transações remotas abertas neste instante (cada uma segura uma conexão)
 * @param ownersWithOpen  donos distintos com ao menos uma transação aberta
 * @param retainedOutcomes desfechos finalizados ainda retidos (para idempotência)
 * @param begun           total de transações abertas
 * @param committed       total comitadas
 * @param rolledBack      total revertidas (por commit-rollback explícito)
 * @param reaped          total revertidas pelo reaper (idle/lifetime — abandono)
 * @param rejectedByLimit total de aberturas rejeitadas por teto (global ou por dono)
 */
public record RemoteTransactionStats(
        int openNow,
        int ownersWithOpen,
        int retainedOutcomes,
        long begun,
        long committed,
        long rolledBack,
        long reaped,
        long rejectedByLimit) {
}
