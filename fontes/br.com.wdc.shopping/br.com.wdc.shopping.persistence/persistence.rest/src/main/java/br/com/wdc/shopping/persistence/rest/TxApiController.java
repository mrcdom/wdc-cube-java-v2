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

    /**
     * Identificador opaco da <b>sessão de cliente</b> (gerado por instância da app cliente). Usado como dono da
     * transação remota quando não há usuário autenticado (segurança desativada) — assim toda transação tem um dono
     * distinguível, mesmo anônima.
     */
    public static final String CLIENT_HEADER = "X-Client-Id";

    private TxApiController() {
        // NOOP
    }

    public static void configure(JavalinConfig config, String prefix) {
        config.routes.post(prefix + "/api/tx/begin", TxApiController::begin);
        config.routes.post(prefix + "/api/tx/commit", TxApiController::commit);
        config.routes.post(prefix + "/api/tx/rollback", TxApiController::rollback);
        config.routes.get(prefix + "/api/tx/status", TxApiController::status);
    }

    private static void begin(Context ctx) {
        SecurityEnforcer.requireAuthenticated();
        var txId = coordinator().begin(currentOwnerKey(ctx));
        ctx.json(Map.of("txId", txId));
    }

    private static void commit(Context ctx) {
        coordinator().commit(txIdHeader(ctx), currentOwnerKey(ctx));
        ctx.json(Map.of("status", "committed"));
    }

    private static void rollback(Context ctx) {
        coordinator().rollback(txIdHeader(ctx), currentOwnerKey(ctx));
        ctx.json(Map.of("status", "rolledback"));
    }

    /** Consulta o estado de uma transação (open/committed/rolledback/unknown) — desambigua resposta perdida. */
    private static void status(Context ctx) {
        ctx.json(Map.of("status", coordinator().status(txIdHeader(ctx), currentOwnerKey(ctx))));
    }

    /**
     * Chave opaca de dono da transação para o solicitante corrente. Em namespaces separados para evitar que um cliente
     * anônimo se passe por (ou colida com) um usuário real:
     * <ul>
     * <li>autenticado → {@code "user:" + userId} (o {@value #CLIENT_HEADER} eventual é <b>ignorado</b>, não-spoofável);</li>
     * <li>sem usuário (segurança desativada) → {@code "anon:" + }{@value #CLIENT_HEADER}, ou {@code null} se o cabeçalho
     * não veio (cliente legado — mantém o comportamento anterior de transação por requisição).</li>
     * </ul>
     * Usada por begin/commit/rollback e pelo decorador {@code transactional} (religar/guarda).
     */
    static String currentOwnerKey(Context ctx) {
        var sc = SecurityContext.CURRENT.get();
        if (sc != null) {
            return "user:" + sc.userId();
        }
        var clientId = ctx.header(CLIENT_HEADER);
        return (clientId != null && !clientId.isBlank()) ? "anon:" + clientId : null;
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
