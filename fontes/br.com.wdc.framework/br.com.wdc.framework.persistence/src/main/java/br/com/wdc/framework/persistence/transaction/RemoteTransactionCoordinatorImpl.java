package br.com.wdc.framework.persistence.transaction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.sql.DataSource;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TransactionLimitExceededException;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;

/**
 * Coordenador de transação remota (lado servidor), apoiado no {@link TransactionScope}.
 *
 * <p>
 * Cada {@code txId} guarda um {@link TransactionScope} <b>suspenso</b> (transação física viva, sem dono de thread). A
 * cada requisição que carrega o {@code txId}, {@link #resume(String)} religa o escopo à thread corrente e
 * {@link #suspend(String)} o desliga ao final, mantendo-o vivo para a próxima. {@link #commit(String)}/
 * {@link #rollback(String)} finalizam e removem.
 * </p>
 *
 * <p>
 * Vale para os dois modos do {@code TransactionScope}: JDBC (conexão única mantida aberta) e JTA (suspend/resume no
 * TransactionManager). Transações ociosas além do timeout são revertidas e removidas (varredura preguiçosa no
 * {@link #begin()}), evitando vazamento de conexão por clientes que abandonam a transação.
 * </p>
 */
public final class RemoteTransactionCoordinatorImpl implements RemoteTransactionCoordinator {

    /** Tempo máximo (ms) ocioso antes de uma transação remota ser revertida e removida. */
    public static final long DEFAULT_IDLE_TIMEOUT_MS = 60_000L;
    /**
     * Tempo de vida <b>absoluto</b> (ms) desde a abertura: além disto a transação é revertida mesmo que ativa,
     * impedindo um cliente que "pinga" antes de cada idle-timeout de segurar uma conexão indefinidamente.
     */
    public static final long DEFAULT_MAX_LIFETIME_MS = 600_000L; // 10 min
    /** Janela (ms) que o desfecho de uma transação finalizada é lembrado, para tornar commit/rollback idempotentes. */
    public static final long DEFAULT_OUTCOME_RETENTION_MS = 300_000L; // 5 min
    /** Teto global de transações remotas abertas (cada uma segura uma conexão) — protege o pool. */
    public static final int DEFAULT_MAX_OPEN = 128;
    /** Teto de transações remotas abertas por dono — impede um cliente monopolizar o pool. */
    public static final int DEFAULT_MAX_OPEN_PER_OWNER = 16;

    private final Supplier<DataSource> dataSourceSupplier;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final long outcomeRetentionMs;
    private final int maxOpen;
    private final int maxOpenPerOwner;
    private final ConcurrentHashMap<String, Entry> registry = new ConcurrentHashMap<>();
    /** Índice dono → nº de transações abertas, para {@link #hasOpenTransactionForOwner} em O(1) (donos nulos não entram). */
    private final ConcurrentHashMap<String, Integer> openByOwner = new ConcurrentHashMap<>();
    /** Desfecho de transações finalizadas (txId → committed/rolledback), retido por {@code outcomeRetentionMs} para idempotência. */
    private final ConcurrentHashMap<String, Finalized> outcomes = new ConcurrentHashMap<>();

    private static final Log LOG = Log.getLogger(RemoteTransactionCoordinatorImpl.class.getSimpleName());

    // Contadores de observabilidade (totais acumulados desde o início).
    private final AtomicLong begunCount = new AtomicLong();
    private final AtomicLong committedCount = new AtomicLong();
    private final AtomicLong rolledBackCount = new AtomicLong();
    private final AtomicLong reapedCount = new AtomicLong();
    private final AtomicLong rejectedByLimitCount = new AtomicLong();

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier) {
        this(dataSourceSupplier, RemoteTransactionOptions.defaults());
    }

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier, RemoteTransactionOptions options) {
        this.dataSourceSupplier = dataSourceSupplier;
        this.idleTimeoutMs = options.idleTimeoutMs();
        this.maxLifetimeMs = options.maxLifetimeMs();
        this.outcomeRetentionMs = options.outcomeRetentionMs();
        this.maxOpen = options.maxOpen();
        this.maxOpenPerOwner = options.maxOpenPerOwner();
    }

    @Override
    public String begin(String ownerKey) {
        reapExpired(); // recupera transações expiradas antes de avaliar os tetos
        enforceLimits(ownerKey);
        try {
            var scope = TransactionScope.beginNew(dataSource()); // CURRENT = scope (owner)
            TransactionScope.suspend(); // desliga da thread; o escopo segue vivo
            var txId = UUID.randomUUID().toString();
            registry.put(txId, new Entry(scope, ownerKey));
            trackOwner(ownerKey);
            begunCount.incrementAndGet();
            LOG.debug("tx remota aberta: {} (owner={}, abertas={})", txId, ownerKey, registry.size());
            return txId;
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao abrir transação remota", e);
        }
    }

    @Override
    public void resume(String txId, String ownerKey) {
        var entry = require(txId);
        verifyOwner(entry.owner, ownerKey);
        if (!entry.inUse.compareAndSet(false, true)) {
            throw new TransactionSystemException("Transação remota em uso concorrente: " + txId);
        }
        try {
            TransactionScope.resume(entry.scope);
            entry.touch();
        } catch (RuntimeException e) {
            entry.inUse.set(false);
            throw e;
        }
    }

    @Override
    public void suspend(String txId) {
        var entry = registry.get(txId);
        if (entry == null) {
            return;
        }
        try {
            TransactionScope.suspend();
        } finally {
            entry.touch();
            entry.inUse.set(false);
        }
    }

    @Override
    public void commit(String txId, String ownerKey) {
        finish(txId, ownerKey, false);
    }

    @Override
    public void rollback(String txId, String ownerKey) {
        finish(txId, ownerKey, true);
    }

    @Override
    public String status(String txId, String ownerKey) {
        var entry = registry.get(txId);
        if (entry != null) {
            verifyOwner(entry.owner, ownerKey);
            return "open";
        }
        var prior = outcomes.get(txId);
        if (prior != null) {
            verifyOwner(prior.owner, ownerKey);
            return prior.committed ? "committed" : "rolledback";
        }
        return "unknown";
    }

    @Override
    public boolean exists(String txId) {
        return txId != null && registry.containsKey(txId);
    }

    @Override
    public boolean hasOpenTransactionForOwner(String ownerKey) {
        return ownerKey != null && openByOwner.containsKey(ownerKey);
    }

    @Override
    public RemoteTransactionStats stats() {
        return new RemoteTransactionStats(registry.size(), openByOwner.size(), outcomes.size(), begunCount.get(),
                committedCount.get(), rolledBackCount.get(), reapedCount.get(), rejectedByLimitCount.get());
    }

    // -------------------------------------------------------------------------

    /**
     * Rejeita a abertura se um teto for atingido. Bound <b>soft</b>: sob alta concorrência pode haver leve
     * ultrapassagem (verificação não-atômica), aceitável para proteção do pool — o objetivo é limitar, não contar exato.
     */
    private void enforceLimits(String ownerKey) {
        if (registry.size() >= maxOpen) {
            rejectedByLimitCount.incrementAndGet();
            LOG.warn("teto GLOBAL de transações remotas atingido ({}) — abertura rejeitada", maxOpen);
            throw new TransactionLimitExceededException(
                    "Limite global de transações remotas abertas atingido (" + maxOpen + ")");
        }
        if (ownerKey != null && openByOwner.getOrDefault(ownerKey, 0) >= maxOpenPerOwner) {
            rejectedByLimitCount.incrementAndGet();
            LOG.warn("teto POR DONO de transações remotas atingido ({}) — owner={}", maxOpenPerOwner, ownerKey);
            throw new TransactionLimitExceededException(
                    "Limite de transações remotas abertas por dono atingido (" + maxOpenPerOwner + ")");
        }
    }

    /** Registra +1 transação aberta para o dono (ignora donos nulos). */
    private void trackOwner(String owner) {
        if (owner != null) {
            openByOwner.merge(owner, 1, Integer::sum);
        }
    }

    /** Registra -1 transação para o dono, removendo a entrada ao chegar a zero (ignora donos nulos). */
    private void untrackOwner(String owner) {
        if (owner != null) {
            openByOwner.computeIfPresent(owner, (k, count) -> count <= 1 ? null : count - 1);
        }
    }

    private void finish(String txId, String ownerKey, boolean rollback) {
        var entry = registry.get(txId);
        if (entry != null) {
            verifyOwner(entry.owner, ownerKey);
            if (registry.remove(txId, entry)) {
                untrackOwner(entry.owner);
                closeAndRecord(txId, entry, rollback);
                return;
            }
            // outra thread finalizou entre o get e o remove → trata como retry
        }
        finishAlreadyFinalized(txId, ownerKey, rollback);
    }

    /** Fecha o escopo (commit/rollback) e grava o desfecho para idempotência. */
    private void closeAndRecord(String txId, Entry entry, boolean rollback) {
        try {
            if (rollback) {
                entry.scope.setRollbackOnly();
            }
            TransactionScope.resume(entry.scope);
            entry.scope.close(); // owner: commit (ou rollback se marcado) e fecha a conexão
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao finalizar transação remota " + txId, e);
        }
        outcomes.put(txId, new Finalized(!rollback, entry.owner));
        (rollback ? rolledBackCount : committedCount).incrementAndGet();
        LOG.debug("tx remota finalizada ({}): {} (abertas={})", rollback ? "rollback" : "commit", txId,
                registry.size());
    }

    /** Caminho de retry: a transação não está mais aberta. Idempotente se o desfecho anterior bate com o pedido. */
    private void finishAlreadyFinalized(String txId, String ownerKey, boolean rollback) {
        var prior = outcomes.get(txId);
        if (prior == null) {
            throw new TransactionSystemException("Transação remota desconhecida ou expirada: " + txId);
        }
        verifyOwner(prior.owner, ownerKey);
        if (prior.committed != !rollback) {
            throw new TransactionConflictException("Transação " + txId + " já finalizada como "
                    + (prior.committed ? "committed" : "rolledback") + "; não aceita o desfecho oposto");
        }
        // mesmo desfecho já aplicado → no-op de sucesso (resposta anterior perdida)
    }

    private void reapExpired() {
        var now = System.currentTimeMillis();
        if (!registry.isEmpty()) {
            var idleDeadline = now - idleTimeoutMs;
            for (var e : registry.entrySet()) {
                var entry = e.getValue();
                var idleExpired = entry.lastUsedAt < idleDeadline;
                var lifetimeExpired = (now - entry.createdAt) >= maxLifetimeMs;
                if ((idleExpired || lifetimeExpired) && !entry.inUse.get() && registry.remove(e.getKey(), entry)) {
                    untrackOwner(entry.owner);
                    rollbackQuietly(entry);
                    outcomes.put(e.getKey(), new Finalized(false, entry.owner)); // reaped = rolledback (desambigua o retry)
                    reapedCount.incrementAndGet();
                    LOG.info("tx remota abandonada revertida ({}): {} owner={}",
                            lifetimeExpired ? "lifetime" : "idle", e.getKey(), entry.owner);
                }
            }
        }
        if (!outcomes.isEmpty()) {
            var outcomeDeadline = now - outcomeRetentionMs;
            outcomes.entrySet().removeIf(en -> en.getValue().at < outcomeDeadline);
        }
    }

    private static void rollbackQuietly(Entry entry) {
        try {
            entry.scope.setRollbackOnly();
            TransactionScope.resume(entry.scope);
            entry.scope.close();
        } catch (Exception ignore) {
            // melhor esforço — transação abandonada
        }
    }

    private Entry require(String txId) {
        var entry = registry.get(txId);
        if (entry == null) {
            throw new TransactionSystemException("Transação remota desconhecida ou expirada: " + txId);
        }
        return entry;
    }

    /** Se a transação tem dono (chave não-nula no begin), exige que a operação apresente a mesma chave. */
    private static void verifyOwner(String owner, String requesterKey) {
        if (owner != null && !owner.equals(requesterKey)) {
            throw new AccessDeniedException("Transação remota não pertence ao solicitante");
        }
    }

    private DataSource dataSource() {
        var ds = dataSourceSupplier != null ? dataSourceSupplier.get() : null;
        if (ds == null) {
            throw new TransactionSystemException("DataSource não inicializado para o coordenador remoto");
        }
        return ds;
    }

    private static final class Entry {
        final TransactionScope scope;
        /** Chave opaca do dono (ex.: id do usuário). {@code null} = transação sem dono (segurança desativada). */
        final String owner;
        final AtomicBoolean inUse = new AtomicBoolean(false);
        final long createdAt = System.currentTimeMillis();
        volatile long lastUsedAt = System.currentTimeMillis();

        Entry(TransactionScope scope, String owner) {
            this.scope = scope;
            this.owner = owner;
        }

        void touch() {
            this.lastUsedAt = System.currentTimeMillis();
        }
    }

    /** Desfecho retido de uma transação finalizada, para tornar commit/rollback idempotentes. */
    private static final class Finalized {
        final boolean committed;
        final String owner;
        final long at = System.currentTimeMillis();

        Finalized(boolean committed, String owner) {
            this.committed = committed;
            this.owner = owner;
        }
    }
}
