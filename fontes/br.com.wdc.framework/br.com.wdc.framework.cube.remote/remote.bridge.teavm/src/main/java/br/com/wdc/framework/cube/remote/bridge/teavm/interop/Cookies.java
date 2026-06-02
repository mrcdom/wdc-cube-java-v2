package br.com.wdc.framework.cube.remote.bridge.teavm.interop;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * Cookie manipulation utilities for browser environment (TeaVM).
 * <p>
 * Values are automatically URL-encoded on write and URL-decoded on read.
 */
public final class Cookies {

    private Cookies() {
    }

    /**
     * Reads a cookie value by name, URL-decoding it.
     *
     * @return the decoded value, or {@code null} if not found
     */
    public static String get(String name) {
        var all = document().getCookie();
        if (all == null || all.isEmpty()) return null;
        var pairs = all.split(";");
        for (var pair : pairs) {
            var trimmed = pair.trim();
            if (trimmed.startsWith(name + "=")) {
                var raw = trimmed.substring(name.length() + 1);
                return URLDecoder.decode(raw, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /**
     * Sets a cookie with the given name, URL-encoded value, and path.
     */
    public static void set(String name, String value, String path) {
        var encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
        document().setCookie(name + "=" + encoded + ";path=" + path);
    }

    /**
     * Removes a cookie by setting it to expire in the past.
     */
    public static void remove(String name) {
        document().setCookie(name + "=;path=/;expires=Thu,01 Jan 1970 00:00:00 GMT");
    }

    private static HTMLDocument document() {
        return Window.current().getDocument();
    }
}
