package br.com.wdc.cube.backend.controller;

import java.util.Map;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.persistence.impl.ShoppingDSLContext;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import io.javalin.config.JavalinConfig;

/**
 * Development-only endpoint to reset the database to a known clean state.
 * <p>
 * Exposes a single HTTP endpoint:
 * <ul>
 *   <li><b>POST</b> {@code /__dev/db-reset} — drops and recreates all tables, inserts seed data</li>
 * </ul>
 * <p>
 * Only registered when {@code server.devMode=true} in {@code application.toml}.
 * Useful for integration scenarios and automated tests that need a predictable initial state.
 */
public class DevDbResetController {

    private DevDbResetController() {
        // utility class
    }

    private static final Log LOG = Log.getLogger(DevDbResetController.class);

    public static void configure(JavalinConfig config) {
        config.routes.post("/__dev/db-reset", ctx -> {
            LOG.info("[DevDbReset] Resetting database...");
            // Conexão obtida do DSLContext do módulo (sem holder global de DataSource).
            ShoppingDSLContext.BEAN.get().connection(connection ->
                    new DBCreate().withConnection(connection).withReset().run());
            LOG.info("[DevDbReset] Database reset complete");
            ctx.status(200).json(Map.of("status", "ok"));
        });
        LOG.info("[DevDbReset] Dev DB reset endpoint active: POST /__dev/db-reset");
    }
}
