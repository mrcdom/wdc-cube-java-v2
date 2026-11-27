package br.com.wdc.cube.backend;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/**
 * Takes a request whose {@code Origin} names this same server — the host and port the request was sent to, its
 * {@code Host} header, as a proxy in front passes it — out of the CORS rules: a page served from here loads its own
 * scripts and calls its own API at whatever address the server is reached, without that address in
 * {@code server.cors.allowedHosts}. The {@code Origin} of such a request is hidden from what follows; any other origin
 * is left to the CORS rules.
 */
public class SameOriginFilter implements Filter {

    private static final String ORIGIN = "Origin";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        if (isSameOrigin(http.getHeader(ORIGIN), http.getHeader("Host"))) {
            chain.doFilter(new WithoutOrigin(http), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /** Whether {@code origin} ({@code scheme://host[:port]}) names the host and port of {@code host}. */
    static boolean isSameOrigin(String origin, String host) {
        if (origin == null || host == null || host.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(origin.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        var scheme = uri.getScheme();
        if (uri.getHost() == null || !("http".equals(scheme) || "https".equals(scheme))) {
            return false;
        }
        var defaultPort = "https".equals(scheme) ? 443 : 80;
        var originPort = uri.getPort() < 0 ? defaultPort : uri.getPort();

        var target = host.trim().toLowerCase(Locale.ROOT);
        var colon = target.lastIndexOf(':');
        var targetHost = target;
        var targetPort = defaultPort;
        if (colon > 0 && !target.endsWith("]")) {
            targetHost = target.substring(0, colon);
            try {
                targetPort = Integer.parseInt(target.substring(colon + 1));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return uri.getHost().toLowerCase(Locale.ROOT).equals(targetHost) && originPort == targetPort;
    }

    /** The request without its {@code Origin} header. */
    private static final class WithoutOrigin extends HttpServletRequestWrapper {

        WithoutOrigin(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(String name) {
            return ORIGIN.equalsIgnoreCase(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return ORIGIN.equalsIgnoreCase(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            var names = Collections.list(super.getHeaderNames());
            names.removeIf(ORIGIN::equalsIgnoreCase);
            return Collections.enumeration(names);
        }
    }
}
