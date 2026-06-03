package br.com.wdc.framework.cube.remote.javalin;

import java.security.SecureRandom;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.servlet.http.Cookie;

/**
 * Handles session cookie generation for the SPA entrypoint (index.html).
 * <p>
 * Generates {@code app_id} (signed random token) and {@code app_skey} (server web key)
 * cookies so the client gets a valid, server-signed session ID on the very first page load.
 * <p>
 * Each application module creates its own instance with its own security.
 */
public final class IndexHtmlController {

    private static final SecureRandom RND = new SecureRandom();

    private final RemoteAppSecurity security;

    public IndexHtmlController(RemoteAppSecurity security) {
        this.security = security;
    }

    /**
     * Registers the session cookie handler before index.html for all context paths.
     */
    public void configure(JavalinConfig config, String... contextPaths) {
        for (String ctx : contextPaths) {
            config.routes.before("/" + ctx + "/index.html", this::handle);
        }
    }

    private void handle(Context ctx) {
        ctx.res().setHeader("Cache-Control", "no-cache, no-store");
        ctx.res().setHeader("Pragma", "no-cache");
        ctx.res().setDateHeader("Expires", 0);

        boolean secure = "https".equalsIgnoreCase(ctx.scheme());

        Cookie appIdCookie = new Cookie("app_id", makeAppId());
        appIdCookie.setPath("/");
        appIdCookie.setMaxAge(10);
        appIdCookie.setSecure(secure);
        ctx.res().addCookie(appIdCookie);

        Cookie pubKeyCookie = new Cookie("app_skey", security.getWebKey());
        pubKeyCookie.setPath("/");
        pubKeyCookie.setMaxAge(-1);
        pubKeyCookie.setSecure(secure);
        ctx.res().addCookie(pubKeyCookie);
    }

    private String makeAppId() {
        var b62 = Base62.BEAN;

        var appIdPart1Bytes = new byte[32];
        RND.nextBytes(appIdPart1Bytes);
        String appIdPart1 = b62.encodeToString(appIdPart1Bytes);

        String appIdPart2 = b62.encodeToString(security.signAsHash(appIdPart1.getBytes()));

        return appIdPart1 + "." + appIdPart2;
    }
}
