package br.com.wdc.shopping.view.swt.util;

import org.eclipse.swt.layout.RowData;

/**
 * Static configurator methods for {@link RowData}.
 * <p>
 * All methods receive an existing RowData, apply a mutation, and return it.
 * Usage: {@code import static ...RowDataUtils.*;}
 * <pre>
 * var rd = new RowData();
 * rdSize(rd, 200, 150);
 * widget.setLayoutData(rd);
 * </pre>
 */
public final class RowDataUtils {

    private RowDataUtils() {}

    /** Set width. */
    public static RowData rdWidth(RowData rd, int w) {
        rd.width = w;
        return rd;
    }

    /** Set height. */
    public static RowData rdHeight(RowData rd, int h) {
        rd.height = h;
        return rd;
    }

    /** Set width + height. */
    public static RowData rdSize(RowData rd, int w, int h) {
        rd.width = w;
        rd.height = h;
        return rd;
    }

    /** Set exclude flag. */
    public static RowData rdExclude(RowData rd, boolean value) {
        rd.exclude = value;
        return rd;
    }
}
