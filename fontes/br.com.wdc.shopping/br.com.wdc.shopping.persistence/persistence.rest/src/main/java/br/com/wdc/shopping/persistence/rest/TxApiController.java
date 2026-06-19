package br.com.wdc.shopping.persistence.rest;

import java.util.Map;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Endpoints da <b>transação remota dirigida pelo cliente</b>: {@code begin}/{@code commit}/{@code rollback}.
 *
 * <p>
 * O cliente abre uma transação ({@code begin} → {@code txId}), envia o {@code txId} no cabeçalho {@value #TX_HEADER}
 * nas chamadas de escrita subsequentes (que se juntam à mesma transação física no servidor) e finaliza com
 * {@code commit} ou {@code rollback}.
 * </p>
 *
 * <p>
 * <b>Segurança:</b> os endpoints exigem autenticação (filtro em {@code /api/tx/*}). O <b>dono</b> da transação é o
 * próprio {@link RemoteTransactionCoordinator}: a identidade do usuário (id) é passada como {@code ownerKey} no
 * {@code begin} e revalidada pelo coordenador em resume/commit/rollback — outro usuário não consegue usá-la. Não há
 * estado de propriedade aqui (o ciclo de vida, incl. o reaper, é todo do coordenador).
 * </p>
 */
public final class TxApiController {

    public static final String TX_HEADER = "X-Tx-Id";

    private TxApiController() {
        // NOOP
    }

    public static void configure(JavalinConfig config, String prefix) {
        config.routes.post(prefix + "/api/tx/begin", TxApiController::begin);
        config.routes.post(prefix + "/api/tx/commit", TxApiController::commit);
        config.routes.post(prefix + "/api/tx/rollback", TxApiController::rollback);
    }

    private static void begin(Context ctx) {
        SecurityEnforcer.requireAuthenticated();
        var txId = coordinator().begin(currentOwnerKey());
        ctx.json(Map.of("txId", txId));
    }

    private static void commit(Context ctx) {
        coordinator().commit(txIdHeader(ctx), currentOwnerKey());
        ctx.json(Map.of("status", "committed"));
    }

    private static void rollback(Context ctx) {
        coordinator().rollback(txIdHeader(ctx), currentOwnerKey());
        ctx.json(Map.of("status", "rolledback"));
    }

    /**
     * Chave opaca de dono da transação para o usuário corrente: o id do usuário, ou {@code null} se segurança está
     * desativada (testes/local). Usada também pelo decorador {@code transactional} ao religar uma escrita.
     */
    static String currentOwnerKey() {
        var sc = SecurityContext.CURRENT.get();
        return sc != null ? String.valueOf(sc.userId()) : null;
    }

    private static String txIdHeader(Context ctx) {
        var txId = ctx.header(TX_HEADER);
        if (txId == null || txId.isBlank()) {
            throw new AccessDeniedException("Cabeçalho " + TX_HEADER + " ausente");
        }
        return txId;
    }

    private static RemoteTransactionCoordinator coordinator() {
        var c = RemoteTransactions.COORDINATOR.get();
        if (c == null) {
            throw new IllegalStateException("Coordenador de transação remota não inicializado");
        }
        return c;
    }
}
