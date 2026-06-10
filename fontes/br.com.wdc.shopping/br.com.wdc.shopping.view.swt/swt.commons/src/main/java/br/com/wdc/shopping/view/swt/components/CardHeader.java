package br.com.wdc.shopping.view.swt.components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.view.swt.theme.Theme;

/**
 * A card header with a 40x40 rounded icon box, a title and a subtitle.
 * Reused in CartViewSwt, ReceiptViewSwt, etc.
 */
public class CardHeader extends Composite {

    public CardHeader(Composite parent, String icon, String title, String subtitle) {
        super(parent, SWT.NONE);
        setBackground(Theme.BG_WHITE);

        var layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 10;
        setLayout(layout);

        // Icon box: 40x40, rounded
        var iconBox = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var iconGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        iconGd.widthHint = 40;
        iconGd.heightHint = 40;
        iconBox.setLayoutData(iconGd);
        iconBox.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            gc.setBackground(Theme.BG_WHITE);
            gc.fillRectangle(0, 0, 40, 40);
            gc.setBackground(Theme.BG_ICON_BOX);
            gc.fillRoundRectangle(0, 0, 40, 40, 10, 10);
            gc.setFont(Theme.FONT_ICON);
            gc.setForeground(Theme.PRIMARY_BLUE);
            var ext = gc.textExtent(icon);
            gc.drawText(icon, (40 - ext.x) / 2, (40 - ext.y) / 2, true);
        });

        // Title block
        var titleBlock = new Composite(this, SWT.NONE);
        titleBlock.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleBlock.setBackground(Theme.BG_WHITE);

        var titleLayout = new GridLayout(1, false);
        titleLayout.marginWidth = 0;
        titleLayout.marginHeight = 0;
        titleLayout.verticalSpacing = 2;
        titleBlock.setLayout(titleLayout);

        var titleLabel = new Label(titleBlock, SWT.NONE);
        titleLabel.setText(title);
        titleLabel.setFont(Theme.FONT_TITLE);
        titleLabel.setForeground(Theme.FG_TEXT_DARK);
        titleLabel.setBackground(Theme.BG_WHITE);

        var subtitleLabel = new Label(titleBlock, SWT.NONE);
        subtitleLabel.setText(subtitle);
        subtitleLabel.setFont(Theme.FONT_BANNER_SUBTITLE);
        subtitleLabel.setForeground(Theme.FG_TEXT_SUBTLE);
        subtitleLabel.setBackground(Theme.BG_WHITE);
    }
}
