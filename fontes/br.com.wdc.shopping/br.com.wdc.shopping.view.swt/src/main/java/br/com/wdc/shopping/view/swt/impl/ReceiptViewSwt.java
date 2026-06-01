package br.com.wdc.shopping.view.swt.impl;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.util.Styles;

public class ReceiptViewSwt extends AbstractViewSwt<ReceiptPresenter> {

    private static final NumberFormat PRICE_FORMAT = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    // Monospace fonts for receipt body
    private static final Font FONT_MONO;
    private static final Font FONT_MONO_BOLD;

    // Colors from CSS: --app-accent-light: #e8f1fc
    private static final Color BG_ICON_BOX = new Color(0xE8, 0xF1, 0xFC);
    // Button hover: light gray
    private static final Color BG_BTN_HOVER = new Color(0xEE, 0xEE, 0xEE);
    // Button focus border: blue
    private static final Color BORDER_BTN_FOCUS = Styles.PRIMARY_BLUE;

    static {
        var display = Display.getDefault();
        FONT_MONO = new Font(display, new FontData("Courier New", 12, SWT.NORMAL));
        FONT_MONO_BOLD = new Font(display, new FontData("Courier New", 12, SWT.BOLD));
    }

    private final ReceiptViewState state;
    private boolean notRendered = true;

    public ReceiptViewSwt(ReceiptPresenter presenter) {
        super("receipt", (ShoppingSwtApplication) presenter.app, presenter,
                new Composite(((ShoppingSwtApplication) presenter.app).getOffscreen(), SWT.NONE));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            this.notRendered = false;
            render();
        }
    }

    private void render() {
        var root = this.element;
        root.setBackground(Styles.BG_PAGE);

        var rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = 20;
        rootLayout.marginHeight = 20;
        rootLayout.verticalSpacing = 0;
        root.setLayout(rootLayout);

        // Success banner (conditional)
        if (this.state.notifySuccess) {
            renderSuccessBanner(root);
        }

        // Main card — Canvas that paints rounded border + shadow, also serves as container
        var card = new Canvas(root, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        card.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        var cardLayout = new GridLayout(1, false);
        cardLayout.marginWidth = 32;
        cardLayout.marginHeight = 32;
        cardLayout.verticalSpacing = 20;
        card.setLayout(cardLayout);

        // Paint rounded card background + shadow + border (behind children)
        card.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = card.getClientArea();

            // Fill page bg first (NO_BACKGROUND means we must paint everything)
            gc.setBackground(Styles.BG_PAGE);
            gc.fillRectangle(area);

            int x = 4, y = 2, w = area.width - 8, h = area.height - 6;

            // Shadow (offset 1px down, slightly larger)
            gc.setAlpha(15);
            gc.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_BLACK));
            gc.fillRoundRectangle(x, y + 2, w, h, 24, 24);
            gc.setAlpha(10);
            gc.fillRoundRectangle(x - 1, y + 1, w + 2, h + 2, 26, 26);
            gc.setAlpha(255);

            // White card fill
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRoundRectangle(x, y, w, h, 24, 24);

            // Border
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(x, y, w - 1, h - 1, 24, 24);
        });

        // Card header
        renderHeader(card);

        // Receipt body
        renderBody(card);

        // Back button
        renderBackButton(card);

        root.layout(true, true);
    }

    private void renderSuccessBanner(Composite parent) {
        var banner = new Composite(parent, SWT.NONE);
        var bannerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        bannerGd.verticalIndent = 0;
        banner.setLayoutData(bannerGd);
        banner.setBackground(Styles.BG_SUCCESS);

        var bannerLayout = new GridLayout(2, false);
        bannerLayout.marginWidth = 16;
        bannerLayout.marginHeight = 12;
        bannerLayout.horizontalSpacing = 10;
        banner.setLayout(bannerLayout);

        var iconLabel = new Label(banner, SWT.NONE);
        iconLabel.setText("\u2713");
        iconLabel.setFont(Styles.FONT_BODY_BOLD);
        iconLabel.setForeground(new Color(0x2E, 0x7D, 0x32));
        iconLabel.setBackground(Styles.BG_SUCCESS);

        var msgLabel = new Label(banner, SWT.NONE);
        msgLabel.setText("Compra realizada com sucesso!");
        msgLabel.setFont(Styles.FONT_BODY_BOLD);
        msgLabel.setForeground(new Color(0x2E, 0x7D, 0x32));
        msgLabel.setBackground(Styles.BG_SUCCESS);
        msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void renderHeader(Composite card) {
        var header = new Composite(card, SWT.NONE);
        header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        header.setBackground(Styles.BG_WHITE);

        var headerLayout = new GridLayout(2, false);
        headerLayout.marginWidth = 0;
        headerLayout.marginHeight = 0;
        headerLayout.horizontalSpacing = 10;
        header.setLayout(headerLayout);

        // Icon box: 40x40, bg #e8f1fc, border-radius 10px
        var iconBox = new Canvas(header, SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
        var iconGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        iconGd.widthHint = 40;
        iconGd.heightHint = 40;
        iconBox.setLayoutData(iconGd);
        iconBox.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            // Clear with parent background first (white card)
            gc.setBackground(Styles.BG_WHITE);
            gc.fillRectangle(0, 0, 40, 40);
            // Rounded icon box fill
            gc.setBackground(BG_ICON_BOX);
            gc.fillRoundRectangle(0, 0, 40, 40, 10, 10);
            // Icon centered — use FONT_ICON (14pt) matching CSS 1.1rem
            gc.setFont(Styles.FONT_ICON);
            gc.setForeground(Styles.PRIMARY_BLUE);
            var iconText = Styles.ICON_RECEIPT;
            var extent = gc.textExtent(iconText);
            gc.drawText(iconText, (40 - extent.x) / 2, (40 - extent.y) / 2, true);
        });

        // Title block
        var titleBlock = new Composite(header, SWT.NONE);
        titleBlock.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        titleBlock.setBackground(Styles.BG_WHITE);

        var titleLayout = new GridLayout(1, false);
        titleLayout.marginWidth = 0;
        titleLayout.marginHeight = 0;
        titleLayout.verticalSpacing = 2;
        titleBlock.setLayout(titleLayout);

        // Title: font-size 1.1rem ≈ 15pt bold
        var titleLabel = new Label(titleBlock, SWT.NONE);
        titleLabel.setText("Recibo de Compra");
        titleLabel.setFont(Styles.FONT_BANNER_TITLE);
        titleLabel.setForeground(Styles.FG_TEXT_DARK);
        titleLabel.setBackground(Styles.BG_WHITE);

        // Subtitle: 0.75rem ≈ 10pt
        var subtitleLabel = new Label(titleBlock, SWT.NONE);
        subtitleLabel.setText("WDC Shopping");
        subtitleLabel.setFont(Styles.FONT_BANNER_SUBTITLE);
        subtitleLabel.setForeground(Styles.FG_TEXT_SUBTLE);
        subtitleLabel.setBackground(Styles.BG_WHITE);
    }

    private void renderBody(Composite card) {
        // Inner section: background BG_PAGE, border 1px solid, border-radius 8px
        var body = new Composite(card, SWT.NONE);
        body.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        body.setBackground(Styles.BG_PAGE);

        var bodyLayout = new GridLayout(1, false);
        bodyLayout.marginWidth = 20;
        bodyLayout.marginHeight = 20;
        bodyLayout.verticalSpacing = 10;
        body.setLayout(bodyLayout);

        // Paint rounded border on body
        body.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            var area = body.getClientArea();
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawRoundRectangle(0, 0, area.width - 1, area.height - 1, 8, 8);
        });

        // Date row
        var dateRow = new Composite(body, SWT.NONE);
        dateRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        dateRow.setBackground(Styles.BG_PAGE);

        var dateLayout = new GridLayout(2, false);
        dateLayout.marginWidth = 0;
        dateLayout.marginHeight = 0;
        dateRow.setLayout(dateLayout);

        var dateLabel = new Label(dateRow, SWT.NONE);
        dateLabel.setText("Data:");
        dateLabel.setFont(FONT_MONO);
        dateLabel.setForeground(Styles.FG_TEXT_SUBTLE);
        dateLabel.setBackground(Styles.BG_PAGE);

        var dateValue = new Label(dateRow, SWT.NONE);
        var dateStr = this.state.receipt != null && this.state.receipt.date != null
                ? DATE_FORMAT.format(new Date(this.state.receipt.date))
                : "--";
        dateValue.setText(dateStr);
        dateValue.setFont(FONT_MONO_BOLD);
        dateValue.setForeground(Styles.FG_TEXT_DARK);
        dateValue.setBackground(Styles.BG_PAGE);
        dateValue.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        // Dotted separator (small dots, like CSS border-bottom: 1px dashed)
        var sep1 = new Canvas(body, SWT.NONE);
        var sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sepGd.heightHint = 4;
        sep1.setLayoutData(sepGd);
        sep1.setBackground(Styles.BG_PAGE);
        sep1.addPaintListener(e -> {
            var gc = e.gc;
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.setLineWidth(1);
            // Draw small dots manually
            int y = 2;
            int w = sep1.getBounds().width;
            for (int x = 0; x < w; x += 4) {
                gc.drawPoint(x, y);
                gc.drawPoint(x + 1, y);
            }
        });

        // Table header
        var tableHeader = new Composite(body, SWT.NONE);
        tableHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tableHeader.setBackground(Styles.BG_PAGE);

        var thLayout = new GridLayout(3, false);
        thLayout.marginWidth = 0;
        thLayout.marginHeight = 0;
        tableHeader.setLayout(thLayout);

        var thItem = new Label(tableHeader, SWT.NONE);
        thItem.setText("ITEM");
        thItem.setFont(FONT_MONO_BOLD);
        thItem.setForeground(Styles.FG_TEXT_SUBTLE);
        thItem.setBackground(Styles.BG_PAGE);
        thItem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        var thQtd = new Label(tableHeader, SWT.NONE);
        thQtd.setText("QTD");
        thQtd.setFont(FONT_MONO_BOLD);
        thQtd.setForeground(Styles.FG_TEXT_SUBTLE);
        thQtd.setBackground(Styles.BG_PAGE);
        var thQtdGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        thQtdGd.widthHint = 80;
        thQtd.setLayoutData(thQtdGd);

        var thValor = new Label(tableHeader, SWT.NONE);
        thValor.setText("VALOR");
        thValor.setFont(FONT_MONO_BOLD);
        thValor.setForeground(Styles.FG_TEXT_SUBTLE);
        thValor.setBackground(Styles.BG_PAGE);
        var thValorGd = new GridData(SWT.END, SWT.CENTER, false, false);
        thValorGd.widthHint = 100;
        thValor.setLayoutData(thValorGd);

        // Solid thin separator under header
        var sep2 = new Canvas(body, SWT.NONE);
        var sep2Gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sep2Gd.heightHint = 1;
        sep2.setLayoutData(sep2Gd);
        sep2.setBackground(Styles.BG_PAGE);
        sep2.addPaintListener(e -> {
            var gc = e.gc;
            gc.setForeground(Styles.BORDER_LIGHT);
            gc.drawLine(0, 0, sep2.getBounds().width, 0);
        });

        // Items
        if (this.state.receipt != null && this.state.receipt.items != null) {
            for (var item : this.state.receipt.items) {
                var row = new Composite(body, SWT.NONE);
                row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                row.setBackground(Styles.BG_PAGE);

                var rowLayout = new GridLayout(3, false);
                rowLayout.marginWidth = 0;
                rowLayout.marginHeight = 4;
                row.setLayout(rowLayout);

                var descLabel = new Label(row, SWT.NONE);
                descLabel.setText(item.description != null ? item.description : "");
                descLabel.setFont(FONT_MONO);
                descLabel.setForeground(Styles.FG_TEXT_DARK);
                descLabel.setBackground(Styles.BG_PAGE);
                descLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                var qtdLabel = new Label(row, SWT.NONE);
                qtdLabel.setText(String.valueOf(item.quantity));
                qtdLabel.setFont(FONT_MONO);
                qtdLabel.setForeground(Styles.FG_TEXT_SUBTLE);
                qtdLabel.setBackground(Styles.BG_PAGE);
                var qtdGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
                qtdGd.widthHint = 80;
                qtdLabel.setLayoutData(qtdGd);

                var valLabel = new Label(row, SWT.NONE);
                valLabel.setText(PRICE_FORMAT.format(item.value * item.quantity));
                valLabel.setFont(FONT_MONO_BOLD);
                valLabel.setForeground(Styles.FG_TEXT_DARK);
                valLabel.setBackground(Styles.BG_PAGE);
                var valGd = new GridData(SWT.END, SWT.CENTER, false, false);
                valGd.widthHint = 100;
                valLabel.setLayoutData(valGd);
            }
        }

        // Blue line separator (2px solid accent)
        var blueSep = new Canvas(body, SWT.NONE);
        var blueGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        blueGd.heightHint = 2;
        blueSep.setLayoutData(blueGd);
        blueSep.setBackground(Styles.BG_PAGE);
        blueSep.addPaintListener(e -> {
            var gc = e.gc;
            gc.setBackground(Styles.PRIMARY_BLUE);
            gc.fillRectangle(0, 0, blueSep.getBounds().width, 2);
        });

        // Total row
        var totalRow = new Composite(body, SWT.NONE);
        totalRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        totalRow.setBackground(Styles.BG_PAGE);

        var totalLayout = new GridLayout(2, false);
        totalLayout.marginWidth = 0;
        totalLayout.marginHeight = 8;
        totalRow.setLayout(totalLayout);

        var totalLabel = new Label(totalRow, SWT.NONE);
        totalLabel.setText("TOTAL:");
        totalLabel.setFont(FONT_MONO_BOLD);
        totalLabel.setForeground(Styles.FG_TEXT_DARK);
        totalLabel.setBackground(Styles.BG_PAGE);
        totalLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        var totalValue = new Label(totalRow, SWT.NONE);
        var totalStr = this.state.receipt != null && this.state.receipt.total != null
                ? PRICE_FORMAT.format(this.state.receipt.total)
                : "R$ 0,00";
        totalValue.setText(totalStr);
        totalValue.setFont(Styles.FONT_PRICE);
        totalValue.setForeground(Styles.PRIMARY_BLUE);
        totalValue.setBackground(Styles.BG_PAGE);
        totalValue.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    }

    private void renderBackButton(Composite card) {
        // Canvas button with hover and focus states
        var backBtn = new Canvas(card, SWT.DOUBLE_BUFFERED);
        var btnGd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        btnGd.heightHint = 34;
        btnGd.verticalIndent = 4;
        backBtn.setLayoutData(btnGd);
        backBtn.setBackground(Styles.BG_WHITE);
        backBtn.setCursor(backBtn.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

        // Compute width from text: 12px left padding + text + 12px right padding
        var tmpGc = new org.eclipse.swt.graphics.GC(backBtn);
        tmpGc.setFont(Styles.FONT_BODY);
        var textExtent = tmpGc.textExtent("\u2190 Voltar aos produtos");
        tmpGc.dispose();
        btnGd.widthHint = textExtent.x + 24; // 12px padding each side

        final boolean[] hovered = {false};
        final boolean[] focused = {false};

        backBtn.addPaintListener(e -> {
            var gc = e.gc;
            gc.setAntialias(SWT.ON);
            int w = backBtn.getBounds().width;
            int h = backBtn.getBounds().height;

            // Background
            if (hovered[0]) {
                gc.setBackground(BG_BTN_HOVER);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            } else {
                gc.setBackground(Styles.BG_WHITE);
                gc.fillRoundRectangle(0, 0, w, h, 8, 8);
            }

            // Focus border
            if (focused[0]) {
                gc.setForeground(BORDER_BTN_FOCUS);
                gc.setLineWidth(2);
                gc.drawRoundRectangle(1, 1, w - 3, h - 3, 8, 8);
                gc.setLineWidth(1);
            } else if (hovered[0]) {
                gc.setForeground(Styles.BORDER_LIGHT);
                gc.drawRoundRectangle(0, 0, w - 1, h - 1, 8, 8);
            }

            // Text
            gc.setFont(Styles.FONT_BODY);
            gc.setForeground(Styles.FG_TEXT_DARK);
            var text = "\u2190 Voltar aos produtos";
            var extent = gc.textExtent(text);
            gc.drawText(text, 12, (h - extent.y) / 2, true);
        });

        backBtn.addListener(SWT.MouseEnter, evt -> {
            hovered[0] = true;
            backBtn.redraw();
        });
        backBtn.addListener(SWT.MouseExit, evt -> {
            hovered[0] = false;
            backBtn.redraw();
        });
        backBtn.addListener(SWT.FocusIn, evt -> {
            focused[0] = true;
            backBtn.redraw();
        });
        backBtn.addListener(SWT.FocusOut, evt -> {
            focused[0] = false;
            backBtn.redraw();
        });
        backBtn.addListener(SWT.MouseUp, evt -> {
            safeAction("receipt.onOpenProducts", () -> presenter.onOpenProducts());
        });
        backBtn.addListener(SWT.KeyDown, evt -> {
            if (evt.keyCode == SWT.CR || evt.keyCode == ' ') {
                safeAction("receipt.onOpenProducts", () -> presenter.onOpenProducts());
            }
        });
    }
}
