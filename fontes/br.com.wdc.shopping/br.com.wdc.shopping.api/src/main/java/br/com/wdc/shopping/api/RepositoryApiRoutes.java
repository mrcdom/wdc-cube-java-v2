package br.com.wdc.shopping.api;

import br.com.wdc.shopping.domain.exception.AccessDeniedException;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;
import br.com.wdc.shopping.api.security.SecurityFilter;
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
		var authService = AuthenticationService.BEAN.get();

		if (authService != null) {
			// Endpoints públicos de autenticação
			new AuthApiController(authService).configure(config);

			// Filtro de segurança para endpoints protegidos
			var securityFilter = new SecurityFilter(authService);
			config.routes.before("/api/repo/*", securityFilter::handle);
			config.routes.after("/api/repo/*", ctx -> SecurityContextHolder.clear());
		}

		// Exception handler para AccessDeniedException
		config.routes.exception(AccessDeniedException.class, (e, ctx) -> {
			ctx.status(403);
			ctx.json(java.util.Map.of("error", e.getMessage()));
		});

		// Controllers de entidades
		UserApiController.configure(config);
		ProductApiController.configure(config);
		PurchaseApiController.configure(config);
		PurchaseItemApiController.configure(config);
	}
}
