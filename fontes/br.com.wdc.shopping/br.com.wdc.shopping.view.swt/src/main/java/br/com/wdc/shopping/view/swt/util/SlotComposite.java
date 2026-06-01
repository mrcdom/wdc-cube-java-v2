package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A lightweight slot container that holds exactly one child Control at a time.
 * <p>
 * When a new content is set:
 * <ul>
 *   <li>If there was a previous content AND this slot is still its parent,
 *       the previous content is moved to the offscreen composite.</li>
 *   <li>The new content is reparented to this slot.</li>
 * </ul>
 * When null is set:
 * <ul>
 *   <li>If there was a previous content AND this slot is still its parent,
 *       it is moved to the offscreen composite.</li>
 * </ul>
 * If the previous content's parent is no longer this slot (reparented elsewhere),
 * it is left untouched.
 */
public class SlotComposite extends Composite {

    private final Composite offscreen;
    private Control content;

    public SlotComposite(Composite parent, Composite offscreen) {
        super(parent, SWT.NONE);
        this.offscreen = offscreen;
        this.setLayout(new FillLayout());
    }

    public void setContent(Control newContent) {
        if (this.content == newContent) {
            return;
        }

        // Release previous content to offscreen (only if we are still its parent)
        if (this.content != null && !this.content.isDisposed() && this.content.getParent() == this) {
            this.content.setParent(this.offscreen);
        }

        this.content = newContent;

        // Adopt new content
        if (newContent != null && !newContent.isDisposed()) {
            newContent.setParent(this);
            newContent.setVisible(true);
        }

        this.requestLayout();
    }

    public Control getContent() {
        return this.content;
    }
}
