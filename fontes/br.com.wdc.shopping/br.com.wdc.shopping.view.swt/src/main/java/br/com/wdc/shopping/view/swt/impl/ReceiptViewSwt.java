package br.com.wdc.shopping.view.swt.impl;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.view.swt.AbstractViewSwt;
import br.com.wdc.shopping.view.swt.ShoppingSwtApplication;
import br.com.wdc.shopping.view.swt.theme.Surface;
import br.com.wdc.shopping.view.swt.theme.Theme;
import br.com.wdc.shopping.view.swt.util.SwtDom;
import static br.com.wdc.shopping.view.swt.util.GridDataUtils.*;

public class ReceiptViewSwt extends AbstractViewSwt<ReceiptPresenter> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final ReceiptViewState state;
    private boolean notRendered = true;

    public ReceiptViewSwt(ReceiptPresenter presenter) {
        super("receipt", (ShoppingSwtApplication) presenter.app, presenter);
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
        SwtDom.render(this.element, (dom, root) -> {
            root.setBackground(Theme.BG_PAGE);
            var rootLayout = new GridLayout(1, false);
            rootLayout.marginWidth = 20;
            rootLayout.marginHeight = 20;
            rootLayout.verticalSpacing = 0;
            root.setLayout(rootLayout);

            // Success banner (conditional)
            if (this.state.notifySuccess) {
                renderSuccessBanner(dom);
            }

            // Main card
            dom.card(32, 32, 20, card -> {
                card.setLayoutData(gdFillH(new GridData()));

                dom.cardHeader(Theme.ICON_RECEIPT, "Recibo de Compra", "WDC Shopping");

                renderBody(dom);
                renderBackButton(dom);
            });

            root.layout(true, true);
        });
    }

    private void renderSuccessBanner(SwtDom dom) {
        dom.row(2, banner -> {
            var bannerGd = gdFillH(new GridData());
            bannerGd.verticalIndent = 0;
            banner.setLayoutData(bannerGd);
            banner.setBackground(Theme.BG_SUCCESS);
            var bannerLayout = (GridLayout) banner.getLayout();
            bannerLayout.marginWidth = 16;
            bannerLayout.marginHeight = 12;
            bannerLayout.horizontalSpacing = 10;

            dom.label(lbl -> {
                lbl.setText("\u2713");
                lbl.setFont(Theme.FONT_BODY_BOLD);
                lbl.setForeground(Theme.FG_SUCCESS);
                lbl.setBackground(Theme.BG_SUCCESS);
            });

            dom.label(lbl -> {
                lbl.setText("Compra realizada com sucesso!");
                lbl.setFont(Theme.FONT_BODY_BOLD);
                lbl.setForeground(Theme.FG_SUCCESS);
                lbl.setBackground(Theme.BG_SUCCESS);
                lbl.setLayoutData(gdFillH(new GridData()));
            });
        });
    }

    private void renderBody(SwtDom dom) {
        dom.col(SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND, body -> {
            body.setLayoutData(gdFillH(new GridData()));
            body.setBackground(Theme.BG_PAGE);
            var bodyLayout = (GridLayout) body.getLayout();
            bodyLayout.marginWidth = 20;
            bodyLayout.marginHeight = 20;
            bodyLayout.verticalSpacing = 10;

            body.addPaintListener(Surface.outlinedPanel(body::getClientArea, Theme.BG_PAGE, 8));

            // Date row
            dom.row(2, dateRow -> {
                dateRow.setLayoutData(gdFillH(new GridData()));
                dateRow.setBackground(Theme.BG_PAGE);
                var dateLayout = (GridLayout) dateRow.getLayout();
                dateLayout.marginWidth = 0;
                dateLayout.marginHeight = 0;

                dom.label(lbl -> {
                    lbl.setText("Data:");
                    lbl.setFont(Theme.FONT_MONO);
                    lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                    lbl.setBackground(Theme.BG_PAGE);
                });

                var dateStr = this.state.receipt != null && this.state.receipt.date != null
                        ? dateFormat.format(new Date(this.state.receipt.date))
                        : "--";
                dom.label(lbl -> {
                    lbl.setText(dateStr);
                    lbl.setFont(Theme.FONT_MONO_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_PAGE);
                    lbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
                });
            });

            // Dotted separator
            dom.canvas(SWT.NONE, sep -> {
                var sepGd = new GridData();
                gdFillH(sepGd);
                gdHeight(sepGd, 4);
                sep.setLayoutData(sepGd);
                sep.setBackground(Theme.BG_PAGE);
                sep.addPaintListener(Surface.dottedSeparator());
            });

            // Table header
            dom.row(3, tableHeader -> {
                tableHeader.setLayoutData(gdFillH(new GridData()));
                tableHeader.setBackground(Theme.BG_PAGE);
                var thLayout = (GridLayout) tableHeader.getLayout();
                thLayout.marginWidth = 0;
                thLayout.marginHeight = 0;

                dom.label(lbl -> {
                    lbl.setText("ITEM");
                    lbl.setFont(Theme.FONT_MONO_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                    lbl.setBackground(Theme.BG_PAGE);
                    lbl.setLayoutData(gdFillH(new GridData()));
                });

                dom.label(lbl -> {
                    lbl.setText("QTD");
                    lbl.setFont(Theme.FONT_MONO_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                    lbl.setBackground(Theme.BG_PAGE);
                    var gd = new GridData();
                    gdCenter(gd);
                    gdWidth(gd, 80);
                    lbl.setLayoutData(gd);
                });

                dom.label(lbl -> {
                    lbl.setText("VALOR");
                    lbl.setFont(Theme.FONT_MONO_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                    lbl.setBackground(Theme.BG_PAGE);
                    var gd = new GridData();
                    gdRight(gd);
                    gdWidth(gd, 100);
                    lbl.setLayoutData(gd);
                });
            });

            // Solid separator under header
            dom.canvas(SWT.NONE, sep -> {
                var sepGd = new GridData();
                gdFillH(sepGd);
                gdHeight(sepGd, 1);
                sep.setLayoutData(sepGd);
                sep.setBackground(Theme.BG_PAGE);
                sep.addPaintListener(Surface.solidSeparator());
            });

            // Items
            if (this.state.receipt != null && this.state.receipt.items != null) {
                for (var item : this.state.receipt.items) {
                    dom.row(3, row -> {
                        row.setLayoutData(gdFillH(new GridData()));
                        row.setBackground(Theme.BG_PAGE);
                        var rowLayout = (GridLayout) row.getLayout();
                        rowLayout.marginWidth = 0;
                        rowLayout.marginHeight = 4;

                        dom.label(lbl -> {
                            lbl.setText(item.description != null ? item.description : "");
                            lbl.setFont(Theme.FONT_MONO);
                            lbl.setForeground(Theme.FG_TEXT_DARK);
                            lbl.setBackground(Theme.BG_PAGE);
                            lbl.setLayoutData(gdFillH(new GridData()));
                        });

                        dom.label(lbl -> {
                            lbl.setText(String.valueOf(item.quantity));
                            lbl.setFont(Theme.FONT_MONO);
                            lbl.setForeground(Theme.FG_TEXT_SUBTLE);
                            lbl.setBackground(Theme.BG_PAGE);
                            var gd = new GridData();
                            gdCenter(gd);
                            gdWidth(gd, 80);
                            lbl.setLayoutData(gd);
                        });

                        dom.label(lbl -> {
                            lbl.setText(Theme.formatPrice(item.value * item.quantity));
                            lbl.setFont(Theme.FONT_MONO_BOLD);
                            lbl.setForeground(Theme.FG_TEXT_DARK);
                            lbl.setBackground(Theme.BG_PAGE);
                            var gd = new GridData();
                            gdRight(gd);
                            gdWidth(gd, 100);
                            lbl.setLayoutData(gd);
                        });
                    });
                }
            }

            // Blue accent separator
            dom.accentLine(2, 0);

            // Total row
            dom.row(2, totalRow -> {
                totalRow.setLayoutData(gdFillH(new GridData()));
                totalRow.setBackground(Theme.BG_PAGE);
                var totalLayout = (GridLayout) totalRow.getLayout();
                totalLayout.marginWidth = 0;
                totalLayout.marginHeight = 8;

                dom.label(lbl -> {
                    lbl.setText("TOTAL:");
                    lbl.setFont(Theme.FONT_MONO_BOLD);
                    lbl.setForeground(Theme.FG_TEXT_DARK);
                    lbl.setBackground(Theme.BG_PAGE);
                    lbl.setLayoutData(gdFillH(new GridData()));
                });

                var totalStr = this.state.receipt != null && this.state.receipt.total != null
                        ? Theme.formatPrice(this.state.receipt.total)
                        : "R$ 0,00";
                dom.label(lbl -> {
                    lbl.setText(totalStr);
                    lbl.setFont(Theme.FONT_PRICE);
                    lbl.setForeground(Theme.PRIMARY_BLUE);
                    lbl.setBackground(Theme.BG_PAGE);
                    lbl.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
                });
            });
        });
    }

    private void renderBackButton(SwtDom dom) {
        dom.actionButton(Theme.ICON_ARROW_LEFT, "Voltar aos produtos", Theme.BG_WHITE, btn -> {
            var gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
            gd.verticalIndent = 4;
            btn.setLayoutData(gd);
            btn.addListener(SWT.MouseUp, evt -> safeAction("receipt.onOpenProducts", presenter::onOpenProducts));
        });
    }
}
