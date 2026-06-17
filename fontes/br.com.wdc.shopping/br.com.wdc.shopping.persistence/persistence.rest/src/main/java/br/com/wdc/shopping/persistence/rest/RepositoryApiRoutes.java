package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.shopping.persistence.rest.doc.RepositoryApiDocs;
import br.com.wdc.shopping.persistence.rest.security.SecurityFilter;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Handler;

/**
 * Registra todos os endpoints REST da API de repositório no Javalin.
 * <p>
 * Se o {@link AuthenticationService} estiver inicializado (via {@code RepositoryBootstrap.initializeSecurity}),
 * registra automaticamente o filtro de segurança e os endpoints de autenticação.
 */
public final class RepositoryApiRoutes {

    private RepositoryApiRoutes() {
        // NOOP
    }

    /**
     * Configura as rotas REST com um prefixo de contexto.
     * <p>
     * Permite registrar a API em {@code /<context>/api/repo/...} para que SPAs servidos em um contexto acessem a API
     * sem CORS.
     */
    public static void configure(JavalinConfig config, String prefix) {
        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            // Endpoints públicos de autenticação
            AuthApiController.configure(config, prefix);

            // Filtro de segurança para endpoints protegidos
            var securityFilter = new SecurityFilter();
            config.routes.before(prefix + "/api/repo/*", securityFilter::handle);
            config.routes.after(prefix + "/api/repo/*", ctx -> SecurityContext.CURRENT.remove());
        }

        // Exception handler para AccessDeniedException
        config.routes.exception(AccessDeniedException.class, (e, ctx) -> {
            ctx.status(403);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        });

        // Controllers de entidades
        UserApiController.configure(config, prefix);
        ProductApiController.configure(config, prefix);
        PurchaseApiController.configure(config, prefix);
        PurchaseItemApiController.configure(config, prefix);

        // OpenAPI document — served at GET {prefix}/openapi.json
        config.routes.get(prefix + "/openapi.json", ctx -> {
            ctx.contentType("application/json");
            ctx.result(RepositoryApiDocs.toJson(prefix));
        });
    }

    /**
     * Envolve um handler de <b>escrita</b> numa transação ({@code TransactionService.required}), de modo que múltiplas
     * statements (ex.: compra + itens) commitem/revertam atomicamente. Em retorno normal commita; em exceção reverte e
     * repropaga a exceção <b>original</b> (tipo preservado), para que os exception mappers do Javalin continuem
     * funcionando. Se nenhum {@link TransactionService} estiver registrado, executa direto.
     */
    static Handler transactional(Handler delegate) {
        return ctx -> {
            var tx = TransactionService.BEAN.get();
            if (tx == null) {
                delegate.handle(ctx);
                return;
            }
            tx.required(t -> {
                try {
                    delegate.handle(ctx);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }
}
