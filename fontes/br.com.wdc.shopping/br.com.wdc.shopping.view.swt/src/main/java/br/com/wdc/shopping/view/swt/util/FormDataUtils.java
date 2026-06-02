package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Control;

/**
 * Static configurator methods for {@link FormData}.
 * <p>
 * All methods receive an existing FormData, apply a mutation, and return it.
 * Usage: {@code import static ...FormDataUtils.*;}
 * <pre>
 * var fd = new FormData();
 * fdTop(fd, 0, 10);
 * fdLeft(fd, 0, 0);
 * fdRight(fd, 100, -10);
 * widget.setLayoutData(fd);
 * </pre>
 */
public final class FormDataUtils {

    private FormDataUtils() {}

    // ===== Size =====

    /** Set width. */
    public static FormData fdWidth(FormData fd, int w) {
        fd.width = w;
        return fd;
    }

    /** Set height. */
    public static FormData fdHeight(FormData fd, int h) {
        fd.height = h;
        return fd;
    }

    // ===== Attachments by percentage + offset =====

    /** Attach top to percentage + offset. */
    public static FormData fdTop(FormData fd, int numerator, int offset) {
        fd.top = new FormAttachment(numerator, offset);
        return fd;
    }

    /** Attach bottom to percentage + offset. */
    public static FormData fdBottom(FormData fd, int numerator, int offset) {
        fd.bottom = new FormAttachment(numerator, offset);
        return fd;
    }

    /** Attach left to percentage + offset. */
    public static FormData fdLeft(FormData fd, int numerator, int offset) {
        fd.left = new FormAttachment(numerator, offset);
        return fd;
    }

    /** Attach right to percentage + offset. */
    public static FormData fdRight(FormData fd, int numerator, int offset) {
        fd.right = new FormAttachment(numerator, offset);
        return fd;
    }

    // ===== Attachments relative to a control =====

    /** Attach top to another control + offset. */
    public static FormData fdTopTo(FormData fd, Control control, int offset) {
        fd.top = new FormAttachment(control, offset, SWT.BOTTOM);
        return fd;
    }

    /** Attach bottom to another control + offset. */
    public static FormData fdBottomTo(FormData fd, Control control, int offset) {
        fd.bottom = new FormAttachment(control, offset, SWT.TOP);
        return fd;
    }

    /** Attach left to another control + offset. */
    public static FormData fdLeftTo(FormData fd, Control control, int offset) {
        fd.left = new FormAttachment(control, offset, SWT.RIGHT);
        return fd;
    }

    /** Attach right to another control + offset. */
    public static FormData fdRightTo(FormData fd, Control control, int offset) {
        fd.right = new FormAttachment(control, offset, SWT.LEFT);
        return fd;
    }

    // ===== Fill shortcuts =====

    /** Attach all edges: fill entire parent with margin. */
    public static FormData fdFill(FormData fd, int margin) {
        fd.top = new FormAttachment(0, margin);
        fd.bottom = new FormAttachment(100, -margin);
        fd.left = new FormAttachment(0, margin);
        fd.right = new FormAttachment(100, -margin);
        return fd;
    }

    /** Attach left + right edges: fill horizontally with margin. */
    public static FormData fdFillH(FormData fd, int margin) {
        fd.left = new FormAttachment(0, margin);
        fd.right = new FormAttachment(100, -margin);
        return fd;
    }

    /** Attach top + bottom edges: fill vertically with margin. */
    public static FormData fdFillV(FormData fd, int margin) {
        fd.top = new FormAttachment(0, margin);
        fd.bottom = new FormAttachment(100, -margin);
        return fd;
    }
}
