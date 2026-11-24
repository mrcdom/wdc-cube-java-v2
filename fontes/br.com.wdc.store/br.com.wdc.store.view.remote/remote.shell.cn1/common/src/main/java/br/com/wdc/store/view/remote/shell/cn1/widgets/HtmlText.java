package br.com.wdc.store.view.remote.shell.cn1.widgets;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.store.view.remote.shell.cn1.util.SimpleHtml;

/**
 * Displays <b>simple</b> HTML (see {@link SimpleHtml}) as one {@link SpanLabel} per line, in a
 * column. {@link #setHtml(String)} re-renders the content — typically called in the view's
 * {@code doUpdate} when the text changes. The (optional) {@code textUIID} styles each line.
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

    /** Clears and recreates one {@link SpanLabel} per line of the simple HTML.
 */
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
