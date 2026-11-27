package br.com.wdc.cube.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.store.design.StoreTokens;
import br.com.wdc.store.domain.StoreConfig;
import br.com.wdc.store.i18n.StoreMessages;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Generates a dynamic landing page listing all available frontend contexts
 * found under the work directory's {@code frontend/}. Each context's description is loaded
 * from {@code frontend/<context>/context.html}. Its texts come from the catalog, in the language of the request's
 * {@code Accept-Language}; its colors and sizes from the design tokens.
 */
public class LandingPageController {

    private static final Log LOG = Log.getLogger(LandingPageController.class);

    public static void configure(JavalinConfig config) {
        var controller = new LandingPageController();
        config.routes.get("/", controller::handle);
        config.routes.get("/index.html", controller::handle);
    }

    private void handle(Context ctx) {
        Path frontendBase = StoreConfig.getFrontendDir();
        var language = StoreMessages.language(firstLanguage(ctx.header("Accept-Language")));
        var html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="%s">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                    <style>
                %s
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: var(--store-font-family-base);
                            background: var(--store-color-background);
                            color: var(--store-color-text);
                            padding: 2rem;
                            max-width: 900px;
                            margin: 0 auto;
                        }
                        h1 { font-size: var(--store-font-size-display); margin-bottom: var(--store-space-xs); }
                        .intro {
                            background: var(--store-color-surface);
                            border-left: 4px solid var(--store-color-primary);
                            padding: var(--store-space-md) var(--store-space-xl);
                            margin-bottom: var(--store-space-xxl);
                            border-radius: var(--store-radius-sm);
                            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
                        }
                        .intro p { line-height: 1.6; color: var(--store-color-text-secondary); }
                        .contexts { display: grid; gap: var(--store-space-md); }
                        .context-card {
                            background: var(--store-color-surface);
                            border-radius: var(--store-radius-md);
                            padding: var(--store-space-xl);
                            box-shadow: 0 2px 6px rgba(0,0,0,0.06);
                            border: 1px solid var(--store-color-border);
                            transition: box-shadow 0.2s, border-color 0.2s;
                        }
                        .context-card:hover {
                            box-shadow: 0 4px 12px rgba(0,0,0,0.12);
                            border-color: var(--store-color-primary);
                        }
                        .context-card h2 { font-size: var(--store-font-size-xl); margin-bottom: var(--store-space-xs); }
                        .context-card h2 a {
                            color: var(--store-color-primary);
                            text-decoration: none;
                        }
                        .context-card h2 a:hover { text-decoration: underline; }
                        .context-card .description { color: var(--store-color-text-secondary); line-height: 1.5; }
                        .no-contexts { color: var(--store-color-text-muted); font-style: italic; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <div class="intro">
                        <p>%s</p>
                    </div>
                    <div class="contexts">
                """.formatted(language, text(language, StoreMessages.LANDING_TITLE), StoreTokens.CSS_VARIABLES,
                text(language, StoreMessages.LANDING_TITLE), text(language, StoreMessages.LANDING_INTRO)));

        if (!Files.isDirectory(frontendBase)) {
            html.append("<p class=\"no-contexts\">").append(text(language, StoreMessages.LANDING_NO_CONTEXTS))
                    .append("</p>");
        } else {
            try (Stream<Path> subdirs = Files.list(frontendBase)) {
                subdirs.filter(Files::isDirectory).sorted().forEach(subdir -> {
                    String contextName = subdir.getFileName().toString();
                    String description = loadDescription(frontendBase, contextName, language);

                    html.append("<div class=\"context-card\">");
                    html.append("<h2><a href=\"/").append(escapeHtml(contextName)).append("/index.html\">");
                    html.append(escapeHtml(contextName));
                    html.append("</a></h2>");
                    html.append("<div class=\"description\">").append(description).append("</div>");
                    html.append("</div>\n");
                });
            } catch (IOException e) {
                LOG.warn("Failed to list frontend contexts", e);
                html.append("<p class=\"no-contexts\">").append(text(language, StoreMessages.LANDING_LIST_ERROR))
                        .append("</p>");
            }
        }

        html.append("""
                    </div>
                </body>
                </html>
                """);

        ctx.contentType("text/html; charset=UTF-8");
        ctx.result(html.toString());
    }

    /** The first language of an {@code Accept-Language} header ({@code pt-BR,pt;q=0.9,en;q=0.8} → {@code pt-BR}). */
    static String firstLanguage(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        var first = acceptLanguage.split(",")[0];
        return first.split(";")[0].trim();
    }

    private static String text(String language, String key) {
        return escapeHtml(StoreMessages.text(language, key));
    }

    /**
     * The description HTML snippet of a context, {@code frontend/<contextName>/context.html}; a fallback when it does
     * not exist.
     */
    private String loadDescription(Path frontendBase, String contextName, String language) {
        Path descFile = frontendBase.resolve(contextName).resolve("context.html");
        if (Files.isRegularFile(descFile)) {
            try {
                return Files.readString(descFile).trim();
            } catch (IOException e) {
                LOG.warn("Failed to read description file: {}", descFile, e);
            }
        }
        return "<em>" + text(language, StoreMessages.LANDING_NO_DESCRIPTION) + "</em>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
