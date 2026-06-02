package br.com.wdc.shopping.backend.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.domain.ShoppingConfig;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Endpoint that proxies and caches external web resources (CDN scripts, fonts, CSS).
 * <p>
 * Route: GET /web-cache/{*url} where url is the full original URL (without scheme prefix).
 * <p>
 * On first request the file is downloaded from the original source and stored under
 * {@code data/web-cache/<host>/<path>}. Subsequent requests are served directly from disk.
 */
public class WebCacheController {

    private static final Log LOG = Log.getLogger(WebCacheController.class);

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry(".js", "application/javascript; charset=utf-8"),
            Map.entry(".mjs", "application/javascript; charset=utf-8"),
            Map.entry(".css", "text/css; charset=utf-8"),
            Map.entry(".html", "text/html; charset=utf-8"),
            Map.entry(".json", "application/json; charset=utf-8"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".woff2", "font/woff2"),
            Map.entry(".ttf", "font/ttf"),
            Map.entry(".otf", "font/otf"),
            Map.entry(".eot", "application/vnd.ms-fontobject"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".map", "application/json"));

    private final Path cacheDir;
    private final HttpClient httpClient;

    public WebCacheController() {
        this.cacheDir = ShoppingConfig.getDataDir().resolve("web-cache");
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static void configure(JavalinConfig config) {
        var controller = new WebCacheController();
        // Use a before handler to intercept all /web-cache/ paths (multi-segment)
        config.routes.before(ctx -> {
            if (ctx.path().startsWith("/web-cache/")) {
                controller.handle(ctx);
                ctx.skipRemainingHandlers();
            }
        });
    }

    protected void handle(Context ctx) {
        // Extract everything after "/web-cache/"
        String encodedUrl = ctx.path().substring("/web-cache/".length());
        if (encodedUrl.isEmpty()) {
            ctx.status(400).result("Missing URL");
            return;
        }

        // Extract host from URL path (first segment)
        int firstSlash = encodedUrl.indexOf('/');
        String host = firstSlash > 0 ? encodedUrl.substring(0, firstSlash) : encodedUrl;

        String queryString = ctx.queryString();
        String sourceUrl = "https://" + encodedUrl + (queryString != null ? "?" + queryString : "");

        // Derive local cache path from URL (host + path + query hash)
        Path localPath = resolveCachePath(encodedUrl, queryString);

        try {
            if (!Files.exists(localPath)) {
                downloadAndCache(sourceUrl, localPath);
            }

            String contentType = guessContentType(localPath.getFileName().toString());
            ctx.contentType(contentType);

            // For CSS and JS module files, rewrite URLs so sub-resources also go through cache
            if (contentType.startsWith("text/css") || contentType.startsWith("application/javascript")) {
                ctx.header("Cache-Control", "public, max-age=86400");
                String content = Files.readString(localPath);
                // Rewrite full URLs: https://host/path → /web-cache/host/path
                content = content.replace("https://", "/web-cache/");
                // Rewrite absolute path imports from the same host: "/path" → "/web-cache/host/path"
                // Handles jspm.dev pattern: import "/npm:..." → import "/web-cache/jspm.dev/npm:..."
                content = rewriteAbsoluteImports(content, host);
                ctx.result(content);
            } else {
                ctx.header("Cache-Control", "public, max-age=31536000, immutable");
                ctx.result(Files.newInputStream(localPath));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Web cache interrupted for: {}", sourceUrl, e);
            ctx.status(502).result("Failed to fetch resource");
        } catch (Exception e) {
            LOG.error("Web cache error for: {}", sourceUrl, e);
            ctx.status(502).result("Failed to fetch resource");
        }
    }

    private void downloadAndCache(String sourceUrl, Path localPath) throws IOException, InterruptedException {
        LOG.info("Downloading and caching: {}", sourceUrl);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sourceUrl))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0 (compatible; WDC-WebCache/1.0)")
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " fetching " + sourceUrl);
        }

        Files.createDirectories(localPath.getParent());
        try (InputStream body = response.body()) {
            Files.copy(body, localPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveCachePath(String urlPath, String queryString) {
        // Sanitize: remove directory traversal attempts
        String sanitized = urlPath.replace("..", "").replaceAll("[<>:\"|?*]", "_");
        Path path = cacheDir.resolve(sanitized);

        // If the path has no recognized file extension, it's a bare package import (e.g., npm:lit@2, base@0.40)
        // Store as _index.js inside a directory to avoid file/directory conflicts
        String fileName = path.getFileName().toString();
        if (!hasFileExtension(fileName)) {
            path = path.resolve("_index.js");
        }

        // If there's a query string, encode it into the filename
        if (queryString != null && !queryString.isEmpty()) {
            fileName = path.getFileName().toString();
            String queryHash = Integer.toHexString(queryString.hashCode());
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) {
                fileName = fileName.substring(0, dotIdx) + "_" + queryHash + fileName.substring(dotIdx);
            } else {
                fileName = fileName + "_" + queryHash;
            }
            path = path.getParent().resolve(fileName);
        }

        // Ensure the resolved path stays within cacheDir (path traversal protection)
        if (!path.normalize().startsWith(cacheDir.normalize())) {
            throw new SecurityException("Path traversal attempt detected");
        }

        return path;
    }

    private static String guessContentType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0) {
            String ext = filename.substring(dot).toLowerCase();
            // Handle hashed filenames (e.g., "elements_abc123.js")
            String mapped = CONTENT_TYPES.get(ext);
            if (mapped != null) {
                return mapped;
            }
        }
        return "application/octet-stream";
    }

    /**
     * Checks if a filename ends with a recognized file extension (not just any dot like version numbers).
     */
    private static boolean hasFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return false;
        }
        String ext = fileName.substring(lastDot).toLowerCase();
        // Known web resource extensions
        return CONTENT_TYPES.containsKey(ext);
    }

    /**
     * Rewrites absolute path imports for JS modules from jspm.dev.
     * <p>
     * jspm.dev uses: import "/npm:@spectrum..." → import "/web-cache/jspm.dev/npm:@spectrum..."
     * Only rewrites paths starting with /npm: to avoid breaking other JS code.
     */
    private static String rewriteAbsoluteImports(String content, String host) {
        if (!host.equals("jspm.dev")) {
            return content;
        }
        String prefix = "/web-cache/" + host;
        // Only rewrite "/npm: and '/npm: patterns (jspm.dev module imports)
        content = content.replace("\"/npm:", "\"" + prefix + "/npm:");
        content = content.replace("'/npm:", "'" + prefix + "/npm:");
        return content;
    }
}
