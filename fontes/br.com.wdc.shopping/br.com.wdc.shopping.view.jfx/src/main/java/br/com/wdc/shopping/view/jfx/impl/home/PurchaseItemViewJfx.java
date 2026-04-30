package br.com.wdc.shopping.view.jfx.impl.home;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.jfx.AbstractViewJfx;
import br.com.wdc.shopping.view.jfx.ShoppingJfxApplication;
import br.com.wdc.shopping.view.jfx.util.JfxDom;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class PurchaseItemViewJfx extends AbstractViewJfx<PurchasesPanelPresenter> {

    private PurchaseInfo state;

    private boolean notRendered = true;
    private Label idElm;
    private long idOldValue;
    private Label dateElm;
    private long dateOldValue;
    private Label itemsElm;
    private String itemsOldValue;
    private Text totalElm;
    private double totalOldValue;

    public PurchaseItemViewJfx(ShoppingJfxApplication app, PurchasesPanelPresenter presenter, int idx) {
        super("purchase-item-" + idx, app, presenter, new VBox());
    }

    public void setState(PurchaseInfo state, boolean scheduleUpdate) {
        this.state = state;
        if (scheduleUpdate) {
            this.update();
        }
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            JfxDom.render((VBox) this.element, this::initialRender);
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
            this.itemsElm.setText(itemsNewValue);
            this.itemsOldValue = itemsNewValue;
        }

        if (this.totalOldValue != this.state.total) {
            this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
            this.totalOldValue = this.state.total;
        }
    }

    @SuppressWarnings("unused")
    private void initialRender(JfxDom dom, VBox pane0) {
        pane0.getStyleClass().add("purchase-item");

        dom.hbox(pane1 -> {
            pane1.getStyleClass().add("order-pnl");

            dom.label(label -> {
                label.setText("Compra");
            });

            dom.label(label -> {
                this.idElm = label;
                this.idElm.setText("#" + this.state.id);
                this.idOldValue = this.state.id;
            });
        });

        dom.vbox(pane1 -> {
            pane1.getStyleClass().add("order-info");

            dom.label(label -> {
                label.setText("Data da compra:");
            });

            dom.label(label -> {
                this.dateElm = label;
                this.dateElm.setText(this.getDateStr());
                this.dateOldValue = this.state.date;
            });

            dom.label(label -> {
                label.setText("Itens adquiridos:");
            });

            dom.label(label -> {
                label.setWrapText(true);
                this.itemsElm = label;
                this.itemsElm.setText(this.getItemsStr());
                this.itemsOldValue = this.itemsElm.getText();
            });

            dom.textFlow(pane2 -> {
                pane2.getStyleClass().add("order-total");

                dom.text(text -> text.setText("Valor Total: "));

                dom.text(text -> {
                    this.totalElm = text;
                    this.totalElm.setText(NumberFormat.getCurrencyInstance().format(this.state.total));
                    this.totalOldValue = this.state.total;
                });
            });

            dom.hbox(_ -> {
                dom.hSpacer();

                dom.button(button -> {
                    button.setText("VEJA MAIS DETALHES");
                    button.setOnAction(this::emitDetailsClicked);
                });
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

    private void emitDetailsClicked(ActionEvent evt) {
        this.presenter.onOpenReceipt(this.state.id);
    }
}
