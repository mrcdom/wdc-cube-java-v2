package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A vertically scrollable page container with standard margins.
 * Sets up root layout + ScrolledComposite + inner content Composite.
 * Call {@link #getContent()} to add children, then {@link #complete()} when done.
 */
public class ScrolledPage {

    private final ScrolledComposite scrolled;
    private final Composite content;

    public ScrolledPage(Composite root) {
        this(root, 20, 20, 0);
    }

    public ScrolledPage(Composite root, int marginWidth, int marginHeight, int verticalSpacing) {
        root.setBackground(Theme.BG_PAGE);
        root.setLayout(new GridLayout(1, false));

        scrolled = new ScrolledComposite(root, SWT.V_SCROLL);
        scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(false);
        scrolled.setBackground(Theme.BG_PAGE);

        content = new Composite(scrolled, SWT.NONE);
        content.setBackground(Theme.BG_PAGE);

        var contentLayout = new GridLayout(1, false);
        contentLayout.marginWidth = marginWidth;
        contentLayout.marginHeight = marginHeight;
        contentLayout.verticalSpacing = verticalSpacing;
        content.setLayout(contentLayout);
    }

    public Composite getContent() {
        return content;
    }

    /**
     * Call after all children have been added to finalize scroll size and layout.
     * Attaches a resize listener so that Label(SWT.WRAP) children rewrap correctly
     * whenever the viewport width changes (including the initial layout pass).
     */
    public void complete() {
        scrolled.setContent(content);
        // Recompute content height using the actual viewport width so that
        // Label(SWT.WRAP) children wrap at the right column width.
        scrolled.addListener(SWT.Resize, e -> {
            int vw = scrolled.getClientArea().width;
            if (vw > 0) {
                content.setSize(content.computeSize(vw, SWT.DEFAULT));
            }
        });
        content.setSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolled.getParent().layout(true, true);
    }
}
