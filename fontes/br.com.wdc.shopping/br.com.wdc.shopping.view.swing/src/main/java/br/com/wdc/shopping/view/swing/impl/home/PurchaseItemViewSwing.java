package br.com.wdc.shopping.view.swing.impl.home;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swing.AbstractViewSwing;
import br.com.wdc.shopping.view.swing.ShoppingSwingApplication;
import br.com.wdc.shopping.view.swing.util.Styles;

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
        super("purchase-item-" + idx, app, presenter, new JPanel(new BorderLayout()));
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
            initialRender();
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

    private void initialRender() {
        this.element.setOpaque(true);
        this.element.setBackground(Styles.BG_WHITE);
        this.element.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(0, 0, 8, 0),
                BorderFactory.createLineBorder(Styles.BORDER_LIGHT, 1)));
        this.element.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Order header
        var orderPnl = new JPanel();
        orderPnl.setLayout(new BoxLayout(orderPnl, BoxLayout.X_AXIS));
        orderPnl.setBackground(Styles.BG_ORDER_HEADER);
        orderPnl.setBorder(new EmptyBorder(6, 10, 6, 10));

        var orderLabel = new JLabel("Compra");
        orderLabel.setFont(Styles.FONT_SMALL_BOLD);
        orderLabel.setForeground(Styles.FG_TEXT);
        orderPnl.add(orderLabel);
        orderPnl.add(Box.createRigidArea(new Dimension(4, 0)));

        this.idElm = new JLabel("#" + this.state.id);
        this.idElm.setFont(Styles.FONT_SMALL_BOLD);
        this.idElm.setForeground(Styles.FG_PRIMARY);
        this.idOldValue = this.state.id;
        orderPnl.add(this.idElm);
        this.element.add(orderPnl, BorderLayout.NORTH);

        // Order info
        var info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(8, 10, 8, 10));

        this.dateElm = new JLabel(this.getDateStr());
        this.dateElm.setFont(Styles.FONT_SMALL);
        this.dateElm.setForeground(Styles.FG_TEXT_SUBTLE);
        this.dateOldValue = this.state.date;
        info.add(this.dateElm);
        info.add(Box.createRigidArea(new Dimension(0, 4)));

        this.itemsElm = new JLabel("<html>" + this.getItemsStr() + "</html>");
        this.itemsElm.setFont(Styles.FONT_SMALL);
        this.itemsElm.setForeground(Styles.FG_TEXT);
        this.itemsOldValue = this.itemsElm.getText();
        info.add(this.itemsElm);
        info.add(Box.createRigidArea(new Dimension(0, 4)));

        this.totalElm = new JLabel(NumberFormat.getCurrencyInstance().format(this.state.total));
        this.totalElm.setFont(Styles.FONT_SMALL_BOLD);
        this.totalElm.setForeground(Styles.FG_TEXT_DARK);
        this.totalOldValue = this.state.total;
        info.add(this.totalElm);
        info.add(Box.createRigidArea(new Dimension(0, 6)));

        var detailsBtn = new JButton("Ver detalhes");
        Styles.styleLinkButton(detailsBtn, Styles.FG_PRIMARY);
        detailsBtn.setAlignmentX(JButton.LEFT_ALIGNMENT);
        detailsBtn.addActionListener(_ -> safeAction("Open receipt", () -> this.presenter.onOpenReceipt(this.state.id)));
        info.add(detailsBtn);

        this.element.add(info, BorderLayout.CENTER);
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
