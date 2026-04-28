package br.com.wdc.shopping.view.react.controller;

import java.security.SecureRandom;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.shopping.view.react.skeleton.util.AppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.servlet.http.Cookie;

public class IndexHtmlController {

	public static void configure(JavalinConfig config) {
		var controller = new IndexHtmlController();
		config.routes.before("/index.html", controller::handle);
	}

	// Singleton instance

	SecureRandom rnd = new SecureRandom();

	protected void handle(Context ctx) {
		// Replicates WdcAppIdFilter: generates and sets app_id + app_skey cookies
		// when the SPA entrypoint is requested so the client gets a valid,
		// server-signed session ID on the very first page load.

		ctx.res().setHeader("Cache-Control", "no-cache, no-store");
		ctx.res().setHeader("Pragma", "no-cache");
		ctx.res().setDateHeader("Expires", 0);

		Cookie appIdCookie = new Cookie("app_id", makeAppId());
		appIdCookie.setPath("/");
		appIdCookie.setMaxAge(10);
		ctx.res().addCookie(appIdCookie);

		Cookie pubKeyCookie = new Cookie("app_skey", AppSecurity.BEAN.getWebKey());
		pubKeyCookie.setPath("/");
		pubKeyCookie.setMaxAge(-1);
		ctx.res().addCookie(pubKeyCookie);
	}

	/**
	 * Generates a server-signed session ID. Format: {randomBase62}.{md5(sign(random))_in_base62} Replicates the logic in WdcAppIdFilter.makeAppId().
	 */
	private String makeAppId() {
		var security = AppSecurity.BEAN;
		var b62 = Base62.BEAN;

		var appIdPart1Bytes = new byte[32];
		rnd.nextBytes(appIdPart1Bytes);
		String appIdPart1 = b62.encodeToString(appIdPart1Bytes);

		String appIdPart2 = b62.encodeToString(security.signAsHash(appIdPart1.getBytes()));

		return appIdPart1 + "." + appIdPart2;
	}

}
