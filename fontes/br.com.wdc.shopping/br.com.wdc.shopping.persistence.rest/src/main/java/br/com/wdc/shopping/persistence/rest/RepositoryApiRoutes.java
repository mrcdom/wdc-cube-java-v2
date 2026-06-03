package br.com.wdc.shopping.persistence.rest;

import br.com.wdc.shopping.domain.exception.AccessDeniedException;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.persistence.rest.security.SecurityFilter;
import io.javalin.config.JavalinConfig;

/**
 * Registra todos os endpoints REST da API de repositório no Javalin.
 * <p>
 * Se o {@link AuthenticationService} estiver inicializado (via
 * {@code RepositoryBootstrap.initializeSecurity}), registra automaticamente
 * o filtro de segurança e os endpoints de autenticação.
 */
public final class RepositoryApiRoutes {

	private RepositoryApiRoutes() {
	}

	/**
	 * Configura as rotas REST.
	 * <p>
	 * Se {@code AuthenticationService.BEAN} estiver populado, habilita
	 * segurança (filtro JWT + endpoints de auth). Caso contrário,
	 * registra apenas os controllers de entidade (modo teste/local).
	 */
	public static void configure(JavalinConfig config) {
		configure(config, "");
	}

	/**
	 * Configura as rotas REST com um prefixo de contexto.
	 * <p>
	 * Permite registrar a API em {@code /<context>/api/repo/...} para
	 * que SPAs servidos em um contexto acessem a API sem CORS.
	 */
	public static void configure(JavalinConfig config, String prefix) {
		var authService = AuthenticationService.BEAN.get();

		if (authService != null) {
			// Endpoints públicos de autenticação
			new AuthApiController(authService).configure(config, prefix);

			// Filtro de segurança para endpoints protegidos
			var securityFilter = new SecurityFilter(authService);
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
	}
}
