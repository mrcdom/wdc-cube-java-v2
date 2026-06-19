package br.com.wdc.framework.persistence.transaction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.sql.DataSource;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
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
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 60_000L;

    private final Supplier<DataSource> dataSourceSupplier;
    private final long idleTimeoutMs;
    private final ConcurrentHashMap<String, Entry> registry = new ConcurrentHashMap<>();

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier) {
        this(dataSourceSupplier, DEFAULT_IDLE_TIMEOUT_MS);
    }

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier, long idleTimeoutMs) {
        this.dataSourceSupplier = dataSourceSupplier;
        this.idleTimeoutMs = idleTimeoutMs;
    }

    @Override
    public String begin(String ownerKey) {
        reapExpired();
        try {
            var scope = TransactionScope.beginNew(dataSource()); // CURRENT = scope (owner)
            TransactionScope.suspend(); // desliga da thread; o escopo segue vivo
            var txId = UUID.randomUUID().toString();
            registry.put(txId, new Entry(scope, ownerKey));
            return txId;
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao abrir transação remota", e);
        }
    }

    @Override
    public void resume(String txId, String ownerKey) {
        var entry = require(txId);
        verifyOwner(entry, ownerKey);
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
    public boolean exists(String txId) {
        return txId != null && registry.containsKey(txId);
    }

    // -------------------------------------------------------------------------

    private void finish(String txId, String ownerKey, boolean rollback) {
        var entry = require(txId);
        verifyOwner(entry, ownerKey);
        registry.remove(txId);
        try {
            if (rollback) {
                entry.scope.setRollbackOnly();
            }
            TransactionScope.resume(entry.scope);
            entry.scope.close(); // owner: commit (ou rollback se marcado) e fecha a conexão
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao finalizar transação remota " + txId, e);
        }
    }

    private void reapExpired() {
        if (registry.isEmpty()) {
            return;
        }
        var deadline = System.currentTimeMillis() - idleTimeoutMs;
        for (var e : registry.entrySet()) {
            var entry = e.getValue();
            if (entry.lastUsedAt < deadline && !entry.inUse.get() && registry.remove(e.getKey(), entry)) {
                rollbackQuietly(entry);
            }
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
    private static void verifyOwner(Entry entry, String ownerKey) {
        if (entry.owner != null && !entry.owner.equals(ownerKey)) {
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
        volatile long lastUsedAt = System.currentTimeMillis();

        Entry(TransactionScope scope, String owner) {
            this.scope = scope;
            this.owner = owner;
        }

        void touch() {
            this.lastUsedAt = System.currentTimeMillis();
        }
    }
}
