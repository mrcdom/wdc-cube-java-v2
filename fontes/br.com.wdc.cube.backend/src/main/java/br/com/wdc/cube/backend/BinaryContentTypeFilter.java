package br.com.wdc.cube.backend;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Servlet filter that strips {@code charset} from content types of binary resources
 * (e.g., WebAssembly {@code .wasm} files). Browsers enforce strict MIME type checking
 * for module scripts and reject {@code application/wasm;charset=utf-8}.
 */
public class BinaryContentTypeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String path = httpReq.getRequestURI();

        if (path.endsWith(".wasm")) {
            HttpServletResponse httpRes = (HttpServletResponse) response;
            chain.doFilter(request, new NoCharsetResponseWrapper(httpRes, "application/wasm"));
        } else if (path.endsWith(".mjs")) {
            HttpServletResponse httpRes = (HttpServletResponse) response;
            chain.doFilter(request, new NoCharsetResponseWrapper(httpRes, "application/javascript"));
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class NoCharsetResponseWrapper extends HttpServletResponseWrapper {
        private final String forcedContentType;

        NoCharsetResponseWrapper(HttpServletResponse response, String forcedContentType) {
            super(response);
            this.forcedContentType = forcedContentType;
        }

        @Override
        public void setContentType(String type) {
            super.setContentType(forcedContentType);
            // Reset charset that Jetty may have added
                super.setCharacterEncoding((String) null);
        }

        @Override
        public String getContentType() {
            return forcedContentType;
        }
    }
}
