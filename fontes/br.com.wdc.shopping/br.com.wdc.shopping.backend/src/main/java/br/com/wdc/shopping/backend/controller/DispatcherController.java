package br.com.wdc.shopping.backend.controller;

import br.com.wdc.framework.commons.log.Log;

import br.com.wdc.shopping.backend.DispatcherHandler;
import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;

public class DispatcherController {

	private static final Log LOG = Log.getLogger(DispatcherController.class);

	/**
	 * Configures WebSocket dispatcher endpoint for frontend-backend communication.
	 */
	public static void configure(JavalinConfig config) {
		var controller = new DispatcherController();
		config.routes.ws("/dispatcher/{id}", ws -> {
			ws.onConnect(controller::onConnect);
			ws.onMessage(controller::onMessage);
			ws.onClose(controller::onClose);
			ws.onError(controller::onError);
		});
		config.routes.ws("/<context>/dispatcher/{id}", ws -> {
			ws.onConnect(controller::onConnect);
			ws.onMessage(controller::onMessage);
			ws.onClose(controller::onClose);
			ws.onError(controller::onError);
		});
	}

	protected void onConnect(WsConnectContext ctx) {
		try {
			String sessionId = ctx.pathParam("id");
			DispatcherHandler handler = DispatcherHandler.getOrCreate(sessionId);
			handler.onConnectOpen(ctx);
			LOG.debug("WebSocket dispatcher connected for session: {}", sessionId);
		} catch (Exception e) {
			LOG.error("Error during WebSocket connect", e);
			try {
				ctx.closeSession();
			} catch (Exception closeErr) {
				LOG.warn("Error closing session", closeErr);
			}
		}
	}

	protected void onMessage(WsMessageContext ctx) {
		try {
			String sessionId = ctx.pathParam("id");
			DispatcherHandler handler = DispatcherHandler.getOrCreate(sessionId);
			handler.onMessage(ctx);
		} catch (Exception e) {
			LOG.error("Error during WebSocket message", e);
		}
	}

	protected void onClose(WsCloseContext ctx) {
		try {
			String sessionId = ctx.pathParam("id");
			DispatcherHandler handler = DispatcherHandler.get(sessionId);
			if (handler != null) {
				handler.onClose(ctx);
			}
		} catch (Exception e) {
			LOG.warn("Error during WebSocket close", e);
		}
	}

	protected void onError(WsErrorContext ctx) {
		try {
			String sessionId = ctx.pathParam("id");
			DispatcherHandler handler = DispatcherHandler.get(sessionId);
			if (handler != null) {
				handler.onError(ctx);
			}
		} catch (Exception e) {
			LOG.error("Error in WebSocket error handler", e);
		}
	}
}
