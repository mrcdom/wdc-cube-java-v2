package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

/**
 * Static configurator methods for {@link GridData}.
 * <p>
 * All methods receive an existing GridData, apply a mutation, and return it.
 * Usage: {@code import static ...GridDataUtils.*;}
 * <pre>
 * // Simple:
 * widget.setLayoutData(gdFill(new GridData()));
 *
 * // Chained:
 * widget.setLayoutData(gdHeight(gdFillH(new GridData()), 36));
 *
 * // For 2+ configurators, prefer var:
 * var gd = gdFillH(new GridData());
 * gd.heightHint = 36;
 * gd.verticalIndent = 12;
 * widget.setLayoutData(gd);
 * </pre>
 */
public final class GridDataUtils {

    private GridDataUtils() {}

    // ===== Alignment Configurators =====

    /** FILL both + grab both. */
    public static GridData gdFill(GridData gd) {
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        return gd;
    }

    /** FILL horizontal, CENTER vertical, grab horizontal. */
    public static GridData gdFillH(GridData gd) {
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.CENTER;
        gd.grabExcessHorizontalSpace = true;
        return gd;
    }

    /** FILL vertical, LEFT horizontal, grab vertical. */
    public static GridData gdFillV(GridData gd) {
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        return gd;
    }

    /** CENTER both axes. */
    public static GridData gdCenter(GridData gd) {
        gd.horizontalAlignment = SWT.CENTER;
        gd.verticalAlignment = SWT.CENTER;
        return gd;
    }

    /** LEFT horizontal, CENTER vertical. */
    public static GridData gdLeft(GridData gd) {
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.CENTER;
        return gd;
    }

    /** RIGHT horizontal, CENTER vertical. */
    public static GridData gdRight(GridData gd) {
        gd.horizontalAlignment = SWT.RIGHT;
        gd.verticalAlignment = SWT.CENTER;
        return gd;
    }

    // ===== Property Configurators =====

    /** Set grabExcessHorizontalSpace + grabExcessVerticalSpace. */
    public static GridData gdGrab(GridData gd) {
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        return gd;
    }

    /** Set grabExcessHorizontalSpace. */
    public static GridData gdGrabH(GridData gd) {
        gd.grabExcessHorizontalSpace = true;
        return gd;
    }

    /** Set grabExcessVerticalSpace. */
    public static GridData gdGrabV(GridData gd) {
        gd.grabExcessVerticalSpace = true;
        return gd;
    }

    /** Set verticalAlignment = TOP. */
    public static GridData gdTop(GridData gd) {
        gd.verticalAlignment = SWT.TOP;
        return gd;
    }

    /** Set verticalAlignment = BOTTOM. */
    public static GridData gdBottom(GridData gd) {
        gd.verticalAlignment = SWT.BOTTOM;
        return gd;
    }

    /** Set widthHint. */
    public static GridData gdWidth(GridData gd, int w) {
        gd.widthHint = w;
        return gd;
    }

    /** Set heightHint. */
    public static GridData gdHeight(GridData gd, int h) {
        gd.heightHint = h;
        return gd;
    }

    /** Set horizontalIndent + verticalIndent. */
    public static GridData gdIndent(GridData gd, int h, int v) {
        gd.horizontalIndent = h;
        gd.verticalIndent = v;
        return gd;
    }

    /** Set horizontalSpan + verticalSpan. */
    public static GridData gdSpan(GridData gd, int h, int v) {
        gd.horizontalSpan = h;
        gd.verticalSpan = v;
        return gd;
    }

    /** Set exclude flag. */
    public static GridData gdExclude(GridData gd, boolean value) {
        gd.exclude = value;
        return gd;
    }
}
