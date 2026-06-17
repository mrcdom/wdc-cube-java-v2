package br.com.wdc.framework.persistence.transaction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import br.com.wdc.framework.domain.transaction.TransactionContext;
import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;

/**
 * Implementação de {@link TransactionService} no estilo CMT do EJB, dual-mode (JTA/JDBC) e reentrante.
 *
 * <p>
 * Apoia-se no frame {@link TransactionScope} (legenda {@code jdbcTx} nos helpers):
 * a propagação REQUIRED junta-se à transação ativa compartilhando a mesma conexão; REQUIRES_NEW/NOT_SUPPORTED suspendem
 * a corrente e retomam ao final (no modo JDBC, abrindo uma segunda conexão).
 * </p>
 *
 * <p>
 * Semântica do {@code work}: COMMIT em retorno normal (apenas o owner comita), ROLLBACK em qualquer exceção (repropagada
 * intacta) ou se {@link TransactionContext#setRollbackOnly()} foi chamado. Falhas ao finalizar a transação viram
 * {@link TransactionSystemException}.
 * </p>
 *
 * <p>
 * <b>Single-thread:</b> a transação é presa à thread (ThreadLocal). Não compartilhe a conexão entre threads.
 * </p>
 */
public final class TransactionServiceImpl implements TransactionService {

    /** Handle entregue ao {@code work}; lê o estado da transação ambiente. */
    private static final TransactionContext HANDLE = new AmbientContext();

    /** DataSource deste contexto (por módulo). Resolução lazy — o backend o injeta no bootstrap do módulo. */
    private final Supplier<DataSource> dataSourceSupplier;

    public TransactionServiceImpl(Supplier<DataSource> dataSourceSupplier) {
        this.dataSourceSupplier = dataSourceSupplier;
    }

    // :: REQUIRED

    @Override
    public void required(Consumer<TransactionContext> work) {
        requiredCall(toFunction(work));
    }

    @Override
    public <T> T requiredCall(Function<TransactionContext, T> work) {
        return finish(beginRequired(), work);
    }

    // :: REQUIRES_NEW

    @Override
    public void requiresNew(Consumer<TransactionContext> work) {
        requiresNewCall(toFunction(work));
    }

    @Override
    public <T> T requiresNewCall(Function<TransactionContext, T> work) {
        var suspended = TransactionScope.suspend();
        try {
            return finish(beginNew(), work);
        } finally {
            TransactionScope.resume(suspended);
        }
    }

    // :: MANDATORY

    @Override
    public void mandatory(Consumer<TransactionContext> work) {
        mandatoryCall(toFunction(work));
    }

    @Override
    public <T> T mandatoryCall(Function<TransactionContext, T> work) {
        if (!txActive()) {
            throw new TransactionRequiredException("mandatory: nenhuma transação ativa no escopo");
        }
        return finish(beginRequired(), work);
    }

    // :: SUPPORTS

    @Override
    public void supports(Consumer<TransactionContext> work) {
        supportsCall(toFunction(work));
    }

    @Override
    public <T> T supportsCall(Function<TransactionContext, T> work) {
        if (txActive()) {
            return finish(beginRequired(), work);
        }
        return work.apply(HANDLE); // sem transação
    }

    // :: NOT_SUPPORTED

    @Override
    public void notSupported(Consumer<TransactionContext> work) {
        notSupportedCall(toFunction(work));
    }

    @Override
    public <T> T notSupportedCall(Function<TransactionContext, T> work) {
        var suspended = TransactionScope.suspend();
        try {
            return work.apply(HANDLE); // sem transação
        } finally {
            TransactionScope.resume(suspended);
        }
    }

    // :: NEVER

    @Override
    public void never(Consumer<TransactionContext> work) {
        neverCall(toFunction(work));
    }

    @Override
    public <T> T neverCall(Function<TransactionContext, T> work) {
        if (txActive()) {
            throw new TransactionNotAllowedException("never: há transação ativa no escopo");
        }
        return work.apply(HANDLE);
    }

    // -------------------------------------------------------------------------
    // :: Núcleo (frame jdbcTx)

    /**
     * Executa o {@code work} no frame de transação: comita em sucesso (somente o owner comita; participante é no-op),
     * faz rollback em exceção (repropagada) e encerra o frame.
     */
    private static <T> T finish(TransactionScope jdbcTx,
            Function<TransactionContext, T> work) {
        T result;
        try {
            result = work.apply(HANDLE);
        } catch (RuntimeException e) {
            jdbcTx.setRollbackOnly();
            closeSwallowing(jdbcTx, e);
            throw e;
        }
        try {
            jdbcTx.close(); // owner: commit (se não marcado) ou rollback; participante: no-op
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao finalizar a transação", e);
        }
        return result;
    }

    private TransactionScope beginRequired() {
        try {
            return TransactionScope.begin(dataSource());
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao abrir/juntar transação", e);
        }
    }

    private TransactionScope beginNew() {
        try {
            return TransactionScope.beginNew(dataSource());
        } catch (Exception e) {
            throw new TransactionSystemException("Falha ao abrir nova transação", e);
        }
    }

    private static boolean txActive() {
        return TransactionScope.isActive();
    }

    /** Fecha o frame em caminho de erro sem mascarar a exceção original (adiciona como suppressed). */
    private static void closeSwallowing(TransactionScope jdbcTx, Throwable original) {
        try {
            jdbcTx.close();
        } catch (Exception suppressed) {
            original.addSuppressed(suppressed);
        }
    }

    private DataSource dataSource() {
        var ds = dataSourceSupplier != null ? dataSourceSupplier.get() : null;
        if (ds == null) {
            throw new TransactionSystemException("DataSource não inicializado para este TransactionService");
        }
        return ds;
    }

    private static <T> Function<TransactionContext, T> toFunction(Consumer<TransactionContext> work) {
        return tx -> {
            work.accept(tx);
            return null;
        };
    }

    // -------------------------------------------------------------------------
    // :: Handle ambiente entregue ao work

    private static final class AmbientContext implements TransactionContext {

        @Override
        public void setRollbackOnly() {
            var current = TransactionScope.current();
            if (current == null) {
                throw new IllegalStateException("setRollbackOnly: nenhuma transação ativa neste escopo");
            }
            current.setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            var current = TransactionScope.current();
            return current != null && current.isRollbackOnly();
        }

        @Override
        public boolean isActive() {
            return TransactionScope.isActive();
        }
    }
}
