package br.com.wdc.shopping.view.remote.shell.cn1.util;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Interpretador leve para o HTML <b>simples</b> das descrições de produto (ul/li, p, br, pre,
 * mais inline como b/i que são apenas removidos). Sem dependência do {@code HTMLComponent} legado
 * e sem regex (não suportado no CN1): converte em linhas de texto e preenche um container.
 *
 * <p>Blocos viram linhas: cada {@code <li>} ganha um marcador "•"; {@code </p>}, {@code </li>} e
 * {@code <br>} quebram linha. As demais tags são removidas e as entidades HTML, decodificadas.</p>
 */
public final class SimpleHtml {

    private SimpleHtml() {
        // NOOP
    }

    /** Limpa {@code target} e o preenche com a interpretação do {@code html}. */
    public static void render(Container target, String html) {
        render(target, html, null);
    }

    /** Como {@link #render(Container, String)}, estilizando o texto de cada linha com {@code textUIID}. */
    public static void render(Container target, String html, String textUIID) {
        target.removeAll();
        if (html == null || html.isEmpty()) {
            return;
        }

        // 1. marcadores de bloco (antes de remover as tags)
        String t = html
                .replace("<br/>", "\n").replace("<br />", "\n").replace("<br>", "\n")
                .replace("</li>", "\n").replace("<li>", "\n• ")
                .replace("</p>", "\n").replace("</pre>", "\n");

        // 2. remove as demais tags (ul, ol, p, pre, b, i, ...) e decodifica entidades
        t = decodeEntities(stripTags(t));

        // 3. uma SpanLabel por linha não-vazia
        for (String line : splitLines(t)) {
            String s = line.trim();
            if (!s.isEmpty()) {
                SpanLabel span = new SpanLabel(s);
                if (textUIID != null) {
                    span.setTextUIID(textUIID);
                }
                target.add(span);
            }
        }
    }

    /** Container novo já preenchido (conveniência). */
    public static Container render(String html) {
        Container c = new Container(BoxLayout.y());
        render(c, html);
        return c;
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

    private static java.util.List<String> splitLines(String s) {
        java.util.List<String> out = new java.util.ArrayList<>();
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
