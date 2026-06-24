package br.com.wdc.shopping.view.remote.shell.cn1.widgets;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.util.SimpleHtml;

/**
 * Exibe HTML <b>simples</b> (ver {@link SimpleHtml}) como uma {@link SpanLabel} por linha, numa
 * coluna. {@link #setHtml(String)} re-renderiza o conteúdo — tipicamente chamado no {@code doUpdate}
 * da view quando o texto muda. O {@code textUIID} (opcional) estiliza cada linha.
 */
public final class HtmlText extends Container {

    private final String textUIID;

    public HtmlText() {
        this(null);
    }

    public HtmlText(String textUIID) {
        super(BoxLayout.y());
        this.textUIID = textUIID;
    }

    /** Limpa e recria uma {@link SpanLabel} por linha do HTML simples. */
    public void setHtml(String html) {
        removeAll();
        for (String line : SimpleHtml.toLines(html)) {
            SpanLabel span = new SpanLabel(line);
            if (textUIID != null) {
                span.setTextUIID(textUIID);
            }
            add(span);
        }
        revalidate();
    }
}
