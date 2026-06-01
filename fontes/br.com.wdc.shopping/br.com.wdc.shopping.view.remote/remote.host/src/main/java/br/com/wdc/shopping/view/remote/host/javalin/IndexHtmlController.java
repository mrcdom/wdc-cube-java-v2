package br.com.wdc.shopping.view.remote.host.javalin;

import java.security.SecureRandom;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.shopping.view.remote.host.util.AppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.servlet.http.Cookie;

/**
 * Handles the session cookie generation for the SPA entrypoint (index.html).
 * <p>
 * Generates {@code app_id} (signed random token) and {@code app_skey} (server web key)
 * cookies so the client gets a valid, server-signed session ID on the very first page load.
 */
public final class IndexHtmlController {

    private static final SecureRandom RND = new SecureRandom();

    private IndexHtmlController() {
    }

    /**
     * Registers the session cookie handler before {@code /<context>/index.html}.
     */
    public static void configure(JavalinConfig config) {
        config.routes.before("/<context>/index.html", IndexHtmlController::handle);
    }

    private static void handle(Context ctx) {
        ctx.res().setHeader("Cache-Control", "no-cache, no-store");
        ctx.res().setHeader("Pragma", "no-cache");
        ctx.res().setDateHeader("Expires", 0);

        Cookie appIdCookie = new Cookie("app_id", makeAppId());
        appIdCookie.setPath("/");
        appIdCookie.setMaxAge(10);
        appIdCookie.setSecure(true);
        ctx.res().addCookie(appIdCookie);

        Cookie pubKeyCookie = new Cookie("app_skey", AppSecurity.BEAN.getWebKey());
        pubKeyCookie.setPath("/");
        pubKeyCookie.setMaxAge(-1);
        pubKeyCookie.setSecure(true);
        ctx.res().addCookie(pubKeyCookie);
    }

    private static String makeAppId() {
        var security = AppSecurity.BEAN;
        var b62 = Base62.BEAN;

        var appIdPart1Bytes = new byte[32];
        RND.nextBytes(appIdPart1Bytes);
        String appIdPart1 = b62.encodeToString(appIdPart1Bytes);

        String appIdPart2 = b62.encodeToString(security.signAsHash(appIdPart1.getBytes()));

        return appIdPart1 + "." + appIdPart2;
    }
}
