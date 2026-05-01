package br.com.wdc.shopping.api;

import io.javalin.config.JavalinConfig;

/**
 * Registra todos os endpoints REST da API de repositório no Javalin.
 */
public final class RepositoryApiRoutes {

	private RepositoryApiRoutes() {
	}

	public static void configure(JavalinConfig config) {
		UserApiController.configure(config);
		ProductApiController.configure(config);
		PurchaseApiController.configure(config);
		PurchaseItemApiController.configure(config);
	}
}
