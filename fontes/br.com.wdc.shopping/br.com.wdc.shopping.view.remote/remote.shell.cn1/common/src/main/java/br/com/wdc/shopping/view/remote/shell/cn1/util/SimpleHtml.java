package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightweight parser for the <b>simple</b> HTML of product descriptions (ul/li, p, br, pre; inline such
 * as b/i is just removed). Without the legacy {@code HTMLComponent} and without regex (not supported in CN1):
 * converts the HTML into a list of <b>text lines</b>. Rendering (lines → components) belongs to the
 * {@code HtmlText} widget — there is no UI here.
 *
 * <p>Blocks become lines: each {@code <li>} gets a "•" bullet; {@code </p>}, {@code </li>} and
 * {@code <br>} break the line. The other tags are removed and HTML entities decoded.</p>
 */
public final class SimpleHtml {

    private SimpleHtml() {
        // NOOP
    }

    /** Converts the simple HTML into text lines (empty ones are discarded). */
    public static List<String> toLines(String html) {
        if (html == null || html.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. block markers (before removing the tags)
        String t = html
                .replace("<br/>", "\n").replace("<br />", "\n").replace("<br>", "\n")
                .replace("</li>", "\n").replace("<li>", "\n• ")
                .replace("</p>", "\n").replace("</pre>", "\n");

        // 2. removes the other tags (ul, ol, p, pre, b, i, ...) and decodes entities
        t = decodeEntities(stripTags(t));

        // 3. non-empty lines
        List<String> out = new ArrayList<>();
        for (String line : splitLines(t)) {
            String s = line.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    // -- internals --

    private static String stripTags(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inTag = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '<') {
                inTag = true;
            } else if (ch == '>') {
                inTag = false;
            } else if (!inTag) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String decodeEntities(String s) {
        return s
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&"); // last, so as not to re-decode

    }

    private static List<String> splitLines(String s) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }
}
