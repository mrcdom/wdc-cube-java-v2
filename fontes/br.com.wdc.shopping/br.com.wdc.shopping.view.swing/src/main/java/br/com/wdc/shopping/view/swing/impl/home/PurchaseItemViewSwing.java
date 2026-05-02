package br.com.wdc.shopping.view.swing.impl.home;

import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.Styles;
import br.com.wdc.shopping.view.swing.util.SwingDom;

public class PurchaseItemViewSwing extends AbstractViewSwing<PurchasesPanelPresenter> {

    private PurchaseInfo state;

    private boolean notRendered = true;
    private JLabel idElm;
    private long idOldValue;
    private JLabel dateElm;
    private long dateOldValue;
    private JLabel itemsElm;
    private String itemsOldValue;
    private JLabel totalElm;
    private double totalOldValue;

    public PurchaseItemViewSwing(ShoppingSwingApplication app, PurchasesPanelPresenter presenter, int idx) {
        super("purchase-item-" + idx, app, presenter, new JPanel());
        this.element.setLayout(new BoxLayout(this.element, BoxLayout.Y_AXIS));
    }

    public void setState(PurchaseInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    protected void onRebuild() {
        this.notRendered = true;
        this.idElm = null;
        this.idOldValue = 0;
        this.dateElm = null;
        this.dateOldValue = 0;
        this.itemsElm = null;
        this.itemsOldValue = null;
        this.totalElm = null;
        this.totalOldValue = 0;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            SwingDom.render(this.element, this::initialRender);
            this.notRendered = false;
        }

        if (this.idOldValue != this.state.id) {
            this.idElm.setText("#" + this.state.id);
            this.idOldValue = this.state.id;
        }

        if (this.dateOldValue != this.state.date) {
            this.dateElm.setText(this.getDateStr());
            this.dateOldValue = this.state.date;
        }

        var itemsNewValue = this.getItemsStr();
        if (!Objects.equals(this.itemsOldValue, itemsNewValue)) {
            this.itemsElm.setText("<html>" + itemsNewValue + "</html>");
            this.itemsOldValue = itemsNewValue;
        }

        if (this.totalOldValue != this.state.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
            this.totalOldValue = this.state.total;
        }
    }

    private void initialRender(SwingDom dom, JPanel pane0) {
        pane0.setOpaque(true);
        pane0.setBackground(Styles.BG_WHITE);
        pane0.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 0, 8, 0),
                BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1)));
        pane0.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Order header
        dom.hbox(orderPnl -> {
            orderPnl.setOpaque(true);
            orderPnl.setBackground(Styles.BG_ORDER_HEADER);
            orderPnl.setBorder(new EmptyBorder(6, 10, 6, 10));

            dom.label(lbl -> {
                lbl.setText("Compra");
                lbl.setFont(Styles.FONT_SMALL_BOLD);
                lbl.setForeground(Styles.FG_TEXT);
            });

            dom.hSpacer(4);

            dom.label(lbl -> {
                this.idElm = lbl;
                lbl.setText("#" + this.state.id);
                lbl.setFont(Styles.FONT_SMALL_BOLD);
                lbl.setForeground(Styles.FG_PRIMARY);
                this.idOldValue = this.state.id;
            });
        });

        // Order info
        dom.vbox(info -> {
            info.setBorder(new EmptyBorder(8, 10, 8, 10));

            dom.label(lbl -> {
                this.dateElm = lbl;
                lbl.setText(this.getDateStr());
                lbl.setFont(Styles.FONT_SMALL);
                lbl.setForeground(Styles.FG_TEXT_SUBTLE);
                this.dateOldValue = this.state.date;
            });

            dom.vSpacer(4);

            dom.label(lbl -> {
                this.itemsElm = lbl;
                lbl.setText("<html>" + this.getItemsStr() + "</html>");
                lbl.setFont(Styles.FONT_SMALL);
                lbl.setForeground(Styles.FG_TEXT);
                this.itemsOldValue = lbl.getText();
            });

            dom.vSpacer(4);

            dom.label(lbl -> {
                this.totalElm = lbl;
                lbl.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
                lbl.setFont(Styles.FONT_SMALL_BOLD);
                lbl.setForeground(Styles.FG_TEXT_DARK);
                this.totalOldValue = this.state.total;
            });

            dom.vSpacer(6);

            dom.button(detailsBtn -> {
                detailsBtn.setText("Ver detalhes");
                Styles.styleLinkButton(detailsBtn, Styles.FG_PRIMARY);
                detailsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
                detailsBtn.addActionListener(_ -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(this.state.id)));
            });
        });
    }

    private String getDateStr() {
        var date = Instant.ofEpochMilli(this.state.date).atZone(ZoneId.systemDefault()).toLocalDate();
        return date.toString();
    }

    private String getItemsStr() {
        if (this.state.items == null || this.state.items.isEmpty()) {
            return "";
        }
        return String.join("; ", this.state.items);
    }
}
