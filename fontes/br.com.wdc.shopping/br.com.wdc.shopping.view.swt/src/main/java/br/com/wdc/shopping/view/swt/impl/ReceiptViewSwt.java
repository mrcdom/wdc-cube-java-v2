package br.com.wdc.shopping.view.swt.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.components.AccentLine;
import br.com.wdc.shopping.view.swt.components.ActionButton;
import br.com.wdc.shopping.view.swt.components.CardHeader;
import br.com.wdc.shopping.view.swt.components.ShadowCard;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;

public class ReceiptViewSwt extends AbstractViewSwt<ReceiptPresenter> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

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
        root.setBackground(Theme.BG_PAGE);

        var rootLayout = new GridLayout(1, false);
        rootLayout.marginWidth = 20;
        rootLayout.marginHeight = 20;
        rootLayout.verticalSpacing = 0;
        root.setLayout(rootLayout);

        // Success banner (conditional)
        if (this.state.notifySuccess) {
            renderSuccessBanner(root);
        }

        // Main card
        var card = new ShadowCard(root, 32, 32, 20);
        card.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        new CardHeader(card, Theme.ICON_RECEIPT, "Recibo de Compra", "WDC Shopping");

        renderBody(card);
        renderBackButton(card);

        root.layout(true, true);
    }

    private void renderSuccessBanner(Composite parent) {
        var banner = new Composite(parent, SWT.NONE);
        var bannerGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        bannerGd.verticalIndent = 0;
        banner.setLayoutData(bannerGd);
        banner.setBackground(Theme.BG_SUCCESS);

        var bannerLayout = new GridLayout(2, false);
        bannerLayout.marginWidth = 16;
        bannerLayout.marginHeight = 12;
        bannerLayout.horizontalSpacing = 10;
        banner.setLayout(bannerLayout);

        var iconLabel = new Label(banner, SWT.NONE);
        iconLabel.setText("\u2713");
        iconLabel.setFont(Theme.FONT_BODY_BOLD);
        iconLabel.setForeground(Theme.FG_SUCCESS);
        iconLabel.setBackground(Theme.BG_SUCCESS);

        var msgLabel = new Label(banner, SWT.NONE);
        msgLabel.setText("Compra realizada com sucesso!");
        msgLabel.setFont(Theme.FONT_BODY_BOLD);
        msgLabel.setForeground(Theme.FG_SUCCESS);
        msgLabel.setBackground(Theme.BG_SUCCESS);
        msgLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private void renderBody(Composite card) {
        var body = new Composite(card, SWT.NONE);
        body.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        body.setBackground(Theme.BG_PAGE);

        var bodyLayout = new GridLayout(1, false);
        bodyLayout.marginWidth = 20;
        bodyLayout.marginHeight = 20;
        bodyLayout.verticalSpacing = 10;
        body.setLayout(bodyLayout);

        body.addPaintListener(Surface.outlinedPanel(body::getClientArea, null, 8));

        // Date row
        var dateRow = new Composite(body, SWT.NONE);
        dateRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        dateRow.setBackground(Theme.BG_PAGE);

        var dateLayout = new GridLayout(2, false);
        dateLayout.marginWidth = 0;
        dateLayout.marginHeight = 0;
        dateRow.setLayout(dateLayout);

        var dateLabel = new Label(dateRow, SWT.NONE);
        dateLabel.setText("Data:");
        dateLabel.setFont(Theme.FONT_MONO);
        dateLabel.setForeground(Theme.FG_TEXT_SUBTLE);
        dateLabel.setBackground(Theme.BG_PAGE);

        var dateValue = new Label(dateRow, SWT.NONE);
        var dateStr = this.state.receipt != null && this.state.receipt.date != null
                ? dateFormat.format(new Date(this.state.receipt.date))
                : "--";
        dateValue.setText(dateStr);
        dateValue.setFont(Theme.FONT_MONO_BOLD);
        dateValue.setForeground(Theme.FG_TEXT_DARK);
        dateValue.setBackground(Theme.BG_PAGE);
        dateValue.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        // Dotted separator
        var sep1 = new Canvas(body, SWT.NONE);
        var sepGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sepGd.heightHint = 4;
        sep1.setLayoutData(sepGd);
        sep1.setBackground(Theme.BG_PAGE);
        sep1.addPaintListener(Surface.dottedSeparator());

        // Table header
        var tableHeader = new Composite(body, SWT.NONE);
        tableHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        tableHeader.setBackground(Theme.BG_PAGE);

        var thLayout = new GridLayout(3, false);
        thLayout.marginWidth = 0;
        thLayout.marginHeight = 0;
        tableHeader.setLayout(thLayout);

        var thItem = new Label(tableHeader, SWT.NONE);
        thItem.setText("ITEM");
        thItem.setFont(Theme.FONT_MONO_BOLD);
        thItem.setForeground(Theme.FG_TEXT_SUBTLE);
        thItem.setBackground(Theme.BG_PAGE);
        thItem.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        var thQtd = new Label(tableHeader, SWT.NONE);
        thQtd.setText("QTD");
        thQtd.setFont(Theme.FONT_MONO_BOLD);
        thQtd.setForeground(Theme.FG_TEXT_SUBTLE);
        thQtd.setBackground(Theme.BG_PAGE);
        var thQtdGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        thQtdGd.widthHint = 80;
        thQtd.setLayoutData(thQtdGd);

        var thValor = new Label(tableHeader, SWT.NONE);
        thValor.setText("VALOR");
        thValor.setFont(Theme.FONT_MONO_BOLD);
        thValor.setForeground(Theme.FG_TEXT_SUBTLE);
        thValor.setBackground(Theme.BG_PAGE);
        var thValorGd = new GridData(SWT.END, SWT.CENTER, false, false);
        thValorGd.widthHint = 100;
        thValor.setLayoutData(thValorGd);

        // Solid separator under header
        var sep2 = new Canvas(body, SWT.NONE);
        var sep2Gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sep2Gd.heightHint = 1;
        sep2.setLayoutData(sep2Gd);
        sep2.setBackground(Theme.BG_PAGE);
        sep2.addPaintListener(Surface.solidSeparator());

        // Items
        if (this.state.receipt != null && this.state.receipt.items != null) {
            for (var item : this.state.receipt.items) {
                var row = new Composite(body, SWT.NONE);
                row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
                row.setBackground(Theme.BG_PAGE);

                var rowLayout = new GridLayout(3, false);
                rowLayout.marginWidth = 0;
                rowLayout.marginHeight = 4;
                row.setLayout(rowLayout);

                var descLabel = new Label(row, SWT.NONE);
                descLabel.setText(item.description != null ? item.description : "");
                descLabel.setFont(Theme.FONT_MONO);
                descLabel.setForeground(Theme.FG_TEXT_DARK);
                descLabel.setBackground(Theme.BG_PAGE);
                descLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                var qtdLabel = new Label(row, SWT.NONE);
                qtdLabel.setText(String.valueOf(item.quantity));
                qtdLabel.setFont(Theme.FONT_MONO);
                qtdLabel.setForeground(Theme.FG_TEXT_SUBTLE);
                qtdLabel.setBackground(Theme.BG_PAGE);
                var qtdGd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
                qtdGd.widthHint = 80;
                qtdLabel.setLayoutData(qtdGd);

                var valLabel = new Label(row, SWT.NONE);
                valLabel.setText(Theme.formatPrice(item.value * item.quantity));
                valLabel.setFont(Theme.FONT_MONO_BOLD);
                valLabel.setForeground(Theme.FG_TEXT_DARK);
                valLabel.setBackground(Theme.BG_PAGE);
                var valGd = new GridData(SWT.END, SWT.CENTER, false, false);
                valGd.widthHint = 100;
                valLabel.setLayoutData(valGd);
            }
        }

        // Blue accent separator
        new AccentLine(body, 2, 0);

        // Total row
        var totalRow = new Composite(body, SWT.NONE);
        totalRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        totalRow.setBackground(Theme.BG_PAGE);

        var totalLayout = new GridLayout(2, false);
        totalLayout.marginWidth = 0;
        totalLayout.marginHeight = 8;
        totalRow.setLayout(totalLayout);

        var totalLabel = new Label(totalRow, SWT.NONE);
        totalLabel.setText("TOTAL:");
        totalLabel.setFont(Theme.FONT_MONO_BOLD);
        totalLabel.setForeground(Theme.FG_TEXT_DARK);
        totalLabel.setBackground(Theme.BG_PAGE);
        totalLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        var totalValue = new Label(totalRow, SWT.NONE);
        var totalStr = this.state.receipt != null && this.state.receipt.total != null
                ? Theme.formatPrice(this.state.receipt.total)
                : "R$ 0,00";
        totalValue.setText(totalStr);
        totalValue.setFont(Theme.FONT_PRICE);
        totalValue.setForeground(Theme.PRIMARY_BLUE);
        totalValue.setBackground(Theme.BG_PAGE);
        totalValue.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    }

    private void renderBackButton(Composite card) {
        var backBtn = new ActionButton(card, Theme.ICON_ARROW_LEFT, "Voltar aos produtos", Theme.BG_WHITE);
        var gd = (GridData) backBtn.getLayoutData();
        gd.verticalIndent = 4;
        backBtn.addListener(SWT.MouseUp, evt -> safeAction("receipt.onOpenProducts", presenter::onOpenProducts));
    }
}
