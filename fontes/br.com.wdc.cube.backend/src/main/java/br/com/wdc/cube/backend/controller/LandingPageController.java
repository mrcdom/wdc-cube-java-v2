package br.com.wdc.cube.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.domain.ShoppingConfig;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Generates a dynamic landing page listing all available frontend contexts
 * found under {@code {basedir}/frontend/}. Each context's description is loaded
 * from {@code {basedir}/frontend/<context>/context.html}.
 */
public class LandingPageController {

    private static final Log LOG = Log.getLogger(LandingPageController.class);

    public static void configure(JavalinConfig config) {
        var controller = new LandingPageController();
        config.routes.get("/", controller::handle);
        config.routes.get("/index.html", controller::handle);
    }

    private void handle(Context ctx) {
        Path frontendBase = ShoppingConfig.getBaseDir().resolve("frontend");
        var html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>WeDoCode — Frontends Experimentais</title>
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            background: #f5f5f5;
                            color: #333;
                            padding: 2rem;
                            max-width: 900px;
                            margin: 0 auto;
                        }
                        h1 { font-size: 1.8rem; margin-bottom: 0.5rem; color: #1a1a2e; }
                        .intro {
                            background: #fff;
                            border-left: 4px solid #0077b6;
                            padding: 1rem 1.5rem;
                            margin-bottom: 2rem;
                            border-radius: 4px;
                            box-shadow: 0 1px 3px rgba(0,0,0,0.08);
                        }
                        .intro p { line-height: 1.6; color: #555; }
                        .contexts { display: grid; gap: 1rem; }
                        .context-card {
                            background: #fff;
                            border-radius: 8px;
                            padding: 1.5rem;
                            box-shadow: 0 2px 6px rgba(0,0,0,0.06);
                            border: 1px solid #e0e0e0;
                            transition: box-shadow 0.2s, border-color 0.2s;
                        }
                        .context-card:hover {
                            box-shadow: 0 4px 12px rgba(0,0,0,0.12);
                            border-color: #0077b6;
                        }
                        .context-card h2 { font-size: 1.2rem; margin-bottom: 0.5rem; }
                        .context-card h2 a {
                            color: #0077b6;
                            text-decoration: none;
                        }
                        .context-card h2 a:hover { text-decoration: underline; }
                        .context-card .description { color: #666; line-height: 1.5; }
                        .no-contexts { color: #999; font-style: italic; }
                    </style>
                </head>
                <body>
                    <h1>WeDoCode — Frontends Experimentais</h1>
                    <div class="intro">
                        <p>
                            Esta página demonstra tecnologias de renderização que têm como alvo o navegador.
                            Trata-se de experimentações e provas de conceito explorando diferentes abordagens
                            para a camada de apresentação remota do framework Cube MVP.
                        </p>
                    </div>
                    <div class="contexts">
                """);

        if (!Files.isDirectory(frontendBase)) {
            html.append("<p class=\"no-contexts\">Nenhum contexto frontend disponível.</p>");
        } else {
            try (Stream<Path> subdirs = Files.list(frontendBase)) {
                subdirs.filter(Files::isDirectory).sorted().forEach(subdir -> {
                    String contextName = subdir.getFileName().toString();
                    String description = loadDescription(frontendBase, contextName);

                    html.append("<div class=\"context-card\">");
                    html.append("<h2><a href=\"/").append(escapeHtml(contextName)).append("/index.html\">");
                    html.append(escapeHtml(contextName));
                    html.append("</a></h2>");
                    html.append("<div class=\"description\">").append(description).append("</div>");
                    html.append("</div>\n");
                });
            } catch (IOException e) {
                LOG.warn("Failed to list frontend contexts", e);
                html.append("<p class=\"no-contexts\">Erro ao listar contextos frontend.</p>");
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

    /**
     * Loads the description HTML snippet from {@code work/frontend/<contextName>/context.html}.
     * Returns a fallback message if the file does not exist.
     */
    private String loadDescription(Path frontendBase, String contextName) {
        Path descFile = frontendBase.resolve(contextName).resolve("context.html");
        if (Files.isRegularFile(descFile)) {
            try {
                return Files.readString(descFile).trim();
            } catch (IOException e) {
                LOG.warn("Failed to read description file: {}", descFile, e);
            }
        }
        return "<em>Sem descrição disponível.</em>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
