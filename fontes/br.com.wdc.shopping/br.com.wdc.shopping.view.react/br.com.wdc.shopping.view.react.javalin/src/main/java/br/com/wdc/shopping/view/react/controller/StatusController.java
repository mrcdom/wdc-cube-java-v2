package br.com.wdc.shopping.view.react.controller;

import java.util.Map;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

public class StatusController {

	public static void configure(JavalinConfig config) {
		var controller = new StatusController();
		config.routes.get("/health", controller::handle);
	}

	protected void handle(Context ctx) {
		ctx.json(Map.of("status", "UP"));
	}
}
