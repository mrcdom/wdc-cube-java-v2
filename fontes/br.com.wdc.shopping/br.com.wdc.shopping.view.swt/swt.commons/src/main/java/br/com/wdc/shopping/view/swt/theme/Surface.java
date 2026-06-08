package br.com.wdc.shopping.view.swt.theme;

import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/**
 * Reusable surface painting algorithms.
 * <p>
 * Factory methods return {@link PaintListener} instances for direct use with
 * {@code addPaintListener}. Context-dependent parameters use {@link Supplier}
 * to defer evaluation until paint time.
 * <p>
 * {@code drawXxx} methods paint onto an existing GC and are intended for use
 * as building blocks within composite (view-specific) paint methods.
 */
public final class Surface {

    private Surface() {
    }

    // ========== PaintListener FACTORIES ==========

    /** Rounded white card with light border (default radius). */
    public static PaintListener card(Supplier<Rectangle> area) {
        return card(area, Theme.CARD_RADIUS);
    }

    /** Rounded white card with light border and custom radius. */
    public static PaintListener card(Supplier<Rectangle> area, int radius) {
        return e -> drawCard(e.gc, area.get(), radius);
    }

    /** Outlined panel with BG_PAGE fill and light border (radius 8). */
    public static PaintListener outlinedPanel(Supplier<Rectangle> area) {
        return outlinedPanel(area, Theme.BG_PAGE, 8);
    }

    /** Outlined panel with custom background and radius. Null background = border only. */
    public static PaintListener outlinedPanel(Supplier<Rectangle> area, Color background, int radius) {
        return e -> drawOutlinedPanel(e.gc, area.get(), background, radius);
    }

    /** Elevated card with drop shadow (used by ShadowCard). */
    public static PaintListener elevatedCard(Supplier<Rectangle> area) {
        return e -> drawElevatedCard(e.gc, area.get());
    }

    /** Pill-shaped background with fixed color. */
    public static PaintListener pill(Supplier<Rectangle> area, Color background) {
        return e -> drawPill(e.gc, area.get(), background);
    }

    /** Rounded error box with icon + wrapping text. Auto-resizes height if needed. */
    public static PaintListener errorBox(Supplier<Rectangle> area, Supplier<String> message) {
        return e -> {
            var gc = e.gc;
            var bounds = area.get();
            gc.setAntialias(SWT.ON);

            // Red-tinted rounded background
            gc.setBackground(Theme.BG_ERROR);
            gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 8, 8);
            gc.setForeground(Theme.BORDER_ERROR_BOX);
            gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, 8, 8);

            // Icon
            gc.setForeground(Theme.FG_ERROR);
            gc.setFont(Theme.FONT_ICON);
            Point iconSz = gc.textExtent(Theme.ICON_EXCLAMATION_CIRCLE);
            int iconX = 12;
            int iconY = 12;
            gc.drawText(Theme.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);

            // Wrapping text
            String msg = message.get();
            if (msg == null) msg = "";
            int textX = iconX + iconSz.x + 10;
            var tl = new TextLayout(e.display);
            tl.setText(msg);
            tl.setFont(Theme.FONT_BODY);
            tl.setWidth(bounds.width - textX - 12);
            gc.setForeground(Theme.FG_ERROR);
            tl.draw(gc, textX, 12);
            int neededHeight = tl.getBounds().height + 24;
            tl.dispose();

            // Auto-resize
            var widget = (Control) e.widget;
            var layoutData = widget.getLayoutData();
            if (layoutData instanceof GridData gd && neededHeight > bounds.height) {
                gd.heightHint = neededHeight;
                widget.getParent().layout(true, true);
            }
        };
    }

    /** Full-width error banner strip with icon + single-line text. */
    public static PaintListener errorBanner(Supplier<Rectangle> area, Supplier<String> message) {
        return e -> {
            var gc = e.gc;
            var bounds = area.get();
            gc.setAntialias(SWT.ON);

            gc.setBackground(Theme.BG_ERROR);
            gc.fillRectangle(0, 0, bounds.width, bounds.height);
            gc.setForeground(Theme.BORDER_ERROR_BOX);
            gc.drawLine(0, bounds.height - 1, bounds.width, bounds.height - 1);

            // Icon
            gc.setForeground(Theme.FG_ERROR);
            gc.setFont(Theme.FONT_ICON);
            Point iconSz = gc.textExtent(Theme.ICON_EXCLAMATION_CIRCLE);
            int iconX = 16;
            int iconY = (bounds.height - iconSz.y) / 2;
            gc.drawText(Theme.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);

            // Text
            gc.setFont(Theme.FONT_BODY);
            String msg = message.get();
            if (msg == null) msg = "";
            gc.drawText(msg, iconX + iconSz.x + 10, (bounds.height - gc.textExtent(msg).y) / 2, true);
        };
    }

    /** Rounded white field border (for text inputs). */
    public static PaintListener borderedField(Supplier<Rectangle> area) {
        return e -> {
            var gc = e.gc;
            var bounds = area.get();
            gc.setAntialias(SWT.ON);
            gc.setBackground(Theme.BG_WHITE);
            gc.fillRoundRectangle(0, 0, bounds.width, bounds.height, 8, 8);
            gc.setForeground(Theme.BORDER_FIELD);
            gc.drawRoundRectangle(0, 0, bounds.width - 1, bounds.height - 1, 8, 8);
        };
    }

    /** Dotted horizontal separator line (receipt-style). */
    public static PaintListener dottedSeparator() {
        return e -> {
            var gc = e.gc;
            gc.setForeground(Theme.BORDER_LIGHT);
            gc.setLineWidth(1);
            int w = ((Control) e.widget).getBounds().width;
            for (int x = 0; x < w; x += 4) {
                gc.drawPoint(x, 2);
                gc.drawPoint(x + 1, 2);
            }
        };
    }

    /** Solid horizontal separator line. */
    public static PaintListener solidSeparator() {
        return e -> {
            var gc = e.gc;
            gc.setForeground(Theme.BORDER_LIGHT);
            gc.drawLine(0, 0, ((Control) e.widget).getBounds().width, 0);
        };
    }

    // ========== DRAW METHODS (building blocks for composite paints) ==========

    public static void drawCard(GC gc, Rectangle area) {
        drawCard(gc, area, Theme.CARD_RADIUS);
    }

    public static void drawCard(GC gc, Rectangle area, int radius) {
        gc.setAntialias(SWT.ON);
        gc.setBackground(Theme.BG_WHITE);
        gc.fillRoundRectangle(0, 0, area.width, area.height, radius, radius);
        gc.setForeground(Theme.BORDER_LIGHT);
        gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, radius, radius);
    }

    public static void drawOutlinedPanel(GC gc, Rectangle area) {
        drawOutlinedPanel(gc, area, Theme.BG_PAGE, 8);
    }

    public static void drawOutlinedPanel(GC gc, Rectangle area, Color background, int radius) {
        gc.setAntialias(SWT.ON);
        if (background != null) {
            gc.setBackground(background);
            gc.fillRoundRectangle(0, 0, area.width, area.height, radius, radius);
        }
        gc.setForeground(Theme.BORDER_LIGHT);
        gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, radius, radius);
    }

    public static void drawElevatedCard(GC gc, Rectangle area) {
        gc.setAntialias(SWT.ON);
        gc.setBackground(Theme.BG_PAGE);
        gc.fillRectangle(area);

        int x = 4;
        int y = 2;
        int w = area.width - 8;
        int h = area.height - 6;

        // Shadow layers
        gc.setAlpha(15);
        gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
        gc.fillRoundRectangle(x, y + 2, w, h, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);
        gc.setAlpha(10);
        gc.fillRoundRectangle(x - 1, y + 1, w + 2, h + 2, 26, 26);
        gc.setAlpha(255);

        // White card fill
        gc.setBackground(Theme.BG_WHITE);
        gc.fillRoundRectangle(x, y, w, h, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);

        // Border
        gc.setForeground(Theme.BORDER_LIGHT);
        gc.drawRoundRectangle(x, y, w - 1, h - 1, Theme.CARD_RADIUS_LARGE, Theme.CARD_RADIUS_LARGE);
    }

    public static void drawPill(GC gc, Rectangle area, Color background) {
        gc.setAntialias(SWT.ON);
        gc.setBackground(background);
        gc.fillRoundRectangle(0, 0, area.width, area.height, area.height, area.height);
    }
}
