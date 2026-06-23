package br.com.wdc.shopping.persistence.client;

import java.util.function.Consumer;
import java.util.function.Function;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.transaction.TransactionContext;
import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;

/**
 * Implementação cliente (REST) do {@link TransactionService} — coordenador remoto dirigido pelo cliente.
 *
 * <p>
 * {@code required} abre uma transação remota (<b>begin</b> → {@code txId}), propaga o {@code txId} (via
 * {@code HttpTransport.setTransactionIdSupplier} → header {@code X-Tx-Id}) nas chamadas de repositório do bloco — que
 * se juntam à mesma transação física no servidor — e finaliza com <b>commit</b> (retorno normal, salvo
 * {@code setRollbackOnly}) ou <b>rollback</b> (exceção). Assim múltiplas escritas REST tornam-se atômicas.
 * </p>
 *
 * <p>
 * O {@code txId} corrente é preso à thread ({@link ThreadLocal}); aninhar {@code required} reaproveita a transação
 * ativa (participa). Single-thread, como o coordenador servidor.
 * </p>
 */
public final class RestTransactionService implements TransactionService {

    /**
     * Transação remota ativa, presa à thread. O isolamento entre fluxos concorrentes depende de uma <b>invariante</b>:
     * cada ação independente do usuário roda na sua própria thread. No backend e nos clientes JVM isso é trivial
     * (threads reais). No navegador (TeaVM, single-thread com event-loop) vale igual <b>porque</b> (1) cada ação é
     * disparada numa green-thread própria (ex.: {@code safeAction} → {@code new Thread().start()}) e (2) o
     * {@code ThreadLocal} do TeaVM é por-thread (armazena por {@code currentThread().key}). Assim, se um bloco
     * {@code required} suspende num XHR e outra ação roda no event-loop, esta lê seu próprio {@code CURRENT} (vazio) e
     * <b>não</b> herda o {@code txId} suspenso. <b>Cuidado ao refatorar:</b> rodar ações inline na main thread ou
     * reusar threads de um pool quebraria esse isolamento.
     */
    private static final ThreadLocal<TxState> CURRENT = new ThreadLocal<>();
    private static final TransactionContext NO_TX = new NoTxContext();

    private final HttpTransport transport;

    public RestTransactionService(HttpTransport transport) {
        this.transport = transport;
        // O transporte enviará X-Tx-Id sempre que houver transação remota ativa nesta thread.
        transport.setTransactionIdSupplier(() -> {
            var state = CURRENT.get();
            return state != null ? state.txId : null;
        });
    }

    // :: REQUIRED

    @Override
    public void required(Consumer<TransactionContext> work) {
        requiredCall(toFunction(work));
    }

    @Override
    public <T> T requiredCall(Function<TransactionContext, T> work) {
        var existing = CURRENT.get();
        if (existing != null) {
            return work.apply(existing); // já em transação remota: participa
        }
        return runInNewTx(work);
    }

    // :: REQUIRES_NEW

    @Override
    public void requiresNew(Consumer<TransactionContext> work) {
        requiresNewCall(toFunction(work));
    }

    @Override
    public <T> T requiresNewCall(Function<TransactionContext, T> work) {
        var outer = CURRENT.get();
        if (outer != null) {
            CURRENT.remove(); // suspende: chamadas internas não levam o txId externo
        }
        try {
            return runInNewTx(work);
        } finally {
            if (outer != null) {
                CURRENT.set(outer);
            }
        }
    }

    // :: MANDATORY

    @Override
    public void mandatory(Consumer<TransactionContext> work) {
        mandatoryCall(toFunction(work));
    }

    @Override
    public <T> T mandatoryCall(Function<TransactionContext, T> work) {
        var state = CURRENT.get();
        if (state == null) {
            throw new TransactionRequiredException("mandatory: nenhuma transação remota ativa");
        }
        return work.apply(state);
    }

    // :: SUPPORTS

    @Override
    public void supports(Consumer<TransactionContext> work) {
        supportsCall(toFunction(work));
    }

    @Override
    public <T> T supportsCall(Function<TransactionContext, T> work) {
        var state = CURRENT.get();
        return work.apply(state != null ? state : NO_TX);
    }

    // :: NOT_SUPPORTED

    @Override
    public void notSupported(Consumer<TransactionContext> work) {
        notSupportedCall(toFunction(work));
    }

    @Override
    public <T> T notSupportedCall(Function<TransactionContext, T> work) {
        var outer = CURRENT.get();
        if (outer != null) {
            CURRENT.remove(); // suspende: chamadas internas não levam txId
        }
        try {
            return work.apply(NO_TX);
        } finally {
            if (outer != null) {
                CURRENT.set(outer);
            }
        }
    }

    // :: NEVER

    @Override
    public void never(Consumer<TransactionContext> work) {
        neverCall(toFunction(work));
    }

    @Override
    public <T> T neverCall(Function<TransactionContext, T> work) {
        if (CURRENT.get() != null) {
            throw new TransactionNotAllowedException("never: há transação remota ativa");
        }
        return work.apply(NO_TX);
    }

    // -------------------------------------------------------------------------

    private <T> T runInNewTx(Function<TransactionContext, T> work) {
        var state = new TxState(remoteBegin());
        CURRENT.set(state);
        try {
            T result = work.apply(state);
            if (state.rollbackOnly) {
                remoteRollback();
            } else {
                remoteCommit();
            }
            return result;
        } catch (RuntimeException e) {
            remoteRollbackQuietly();
            throw e;
        } finally {
            CURRENT.remove();
        }
    }

    private String remoteBegin() {
        var response = transport.postJson("/api/tx/begin", "{}");
        var reader = new JsonStreamReader(response);
        reader.beginObject();
        var txId = (String) null;
        while (reader.hasNext()) {
            if ("txId".equals(reader.nextName())) {
                txId = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (txId == null || txId.isBlank()) {
            throw new TransactionSystemException("Resposta de begin sem txId");
        }
        return txId;
    }

    /** O txId vai no header X-Tx-Id (transporte), pois CURRENT ainda está setado durante o commit/rollback. */
    private void remoteCommit() {
        transport.postJson("/api/tx/commit", "{}");
    }

    private void remoteRollback() {
        transport.postJson("/api/tx/rollback", "{}");
    }

    private void remoteRollbackQuietly() {
        try {
            remoteRollback();
        } catch (RuntimeException ignore) {
            // melhor esforço: a exceção original do work é a relevante
        }
    }

    private static <T> Function<TransactionContext, T> toFunction(Consumer<TransactionContext> work) {
        return tx -> {
            work.accept(tx);
            return null;
        };
    }

    /** Handle de uma transação remota ativa. */
    private static final class TxState implements TransactionContext {
        final String txId;
        boolean rollbackOnly;

        TxState(String txId) {
            this.txId = txId;
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }

    /** Handle entregue quando não há transação (supports/notSupported/never sem tx ativa). */
    private static final class NoTxContext implements TransactionContext {
        @Override
        public void setRollbackOnly() {
            throw new IllegalStateException("setRollbackOnly: nenhuma transação remota ativa neste escopo");
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
