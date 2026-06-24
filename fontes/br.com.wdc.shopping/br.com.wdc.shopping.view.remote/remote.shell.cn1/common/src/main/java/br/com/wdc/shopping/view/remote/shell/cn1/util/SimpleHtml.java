package br.com.wdc.shopping.view.remote.shell.cn1.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser leve para o HTML <b>simples</b> das descrições de produto (ul/li, p, br, pre; inline como
 * b/i é apenas removido). Sem o {@code HTMLComponent} legado e sem regex (não suportado no CN1):
 * converte o HTML numa lista de <b>linhas de texto</b>. A renderização (linhas → componentes) é do
 * widget {@code HtmlText} — aqui não há UI.
 *
 * <p>Blocos viram linhas: cada {@code <li>} ganha um marcador "•"; {@code </p>}, {@code </li>} e
 * {@code <br>} quebram linha. As demais tags são removidas e as entidades HTML, decodificadas.</p>
 */
public final class SimpleHtml {

    private SimpleHtml() {
        // NOOP
    }

    /** Converte o HTML simples em linhas de texto (as vazias são descartadas). */
    public static List<String> toLines(String html) {
        if (html == null || html.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. marcadores de bloco (antes de remover as tags)
        String t = html
                .replace("<br/>", "\n").replace("<br />", "\n").replace("<br>", "\n")
                .replace("</li>", "\n").replace("<li>", "\n• ")
                .replace("</p>", "\n").replace("</pre>", "\n");

        // 2. remove as demais tags (ul, ol, p, pre, b, i, ...) e decodifica entidades
        t = decodeEntities(stripTags(t));

        // 3. linhas não-vazias
        List<String> out = new ArrayList<>();
        for (String line : splitLines(t)) {
            String s = line.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    // -- internos --

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
                .replace("&amp;", "&"); // por último para não re-decodificar
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
