package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.TextLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * An error banner with rounded corners, red tinted background, icon and message text.
 * Used in CartViewSwt, ProductViewSwt, LoginViewSwt, etc.
 */
public class ErrorBanner extends Canvas {

    private String message;
    private final GridData gridData;

    public ErrorBanner(Composite parent, int verticalIndent) {
        this(parent, verticalIndent, true);
    }

    public ErrorBanner(Composite parent, int verticalIndent, boolean initiallyVisible) {
        super(parent, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);

        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.heightHint = 40;
        gridData.verticalIndent = verticalIndent;
        if (!initiallyVisible) {
            gridData.exclude = true;
            setVisible(false);
        }
        setLayoutData(gridData);

        addPaintListener(ev -> {
            var gc = ev.gc;
            var area = getClientArea();
            gc.setAntialias(SWT.ON);

            // White background behind banner
            gc.setBackground(Theme.BG_WHITE);
            gc.fillRectangle(0, 0, area.width, area.height);

            // Red-tinted background with rounded corners
            gc.setBackground(Theme.BG_ERROR);
            gc.fillRoundRectangle(0, 0, area.width, area.height, 8, 8);

            // Red border
            gc.setForeground(Theme.BORDER_ERROR_BOX);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);

            // Icon
            gc.setForeground(Theme.FG_ERROR);
            gc.setFont(Theme.FONT_ICON);
            var iconSz = gc.textExtent(Theme.ICON_EXCLAMATION_CIRCLE);
            int iconX = 12;
            int iconY = (area.height - iconSz.y) / 2;
            gc.drawText(Theme.ICON_EXCLAMATION_CIRCLE, iconX, iconY, true);

            // Error text
            String msg = message != null ? message : "";
            int textX = iconX + iconSz.x + 10;
            var tl = new TextLayout(ev.display);
            tl.setText(msg);
            tl.setFont(Theme.FONT_BODY);
            tl.setWidth(area.width - textX - 12);
            gc.setForeground(Theme.FG_ERROR);
            int textY = (area.height - tl.getBounds().height) / 2;
            tl.draw(gc, textX, textY);

            // Resize height if needed
            int neededHeight = tl.getBounds().height + 24;
            tl.dispose();
            if (neededHeight > area.height) {
                gridData.heightHint = neededHeight;
                getParent().layout(true, true);
            }
        });
    }

    public void setMessage(String message) {
        this.message = message;
        redraw();
    }

    public String getMessage() {
        return message;
    }

    /**
     * Shows or hides the banner, toggling GridData.exclude and visibility.
     * Triggers a parent re-layout.
     */
    public void setShown(boolean shown) {
        gridData.exclude = !shown;
        setVisible(shown);
        getParent().layout(true, true);
    }

    public boolean isShown() {
        return !gridData.exclude;
    }
}
