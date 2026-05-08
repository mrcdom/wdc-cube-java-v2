package br.com.wdc.shopping.view.teavm.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class ReceiptViewTeaVM extends AbstractViewTeaVM<ReceiptPresenter> {

    private final ReceiptViewState state;

    private HTMLElement dateElm;
    private String dateOldValue;
    private HTMLElement totalElm;
    private String totalOldValue;
    private HTMLElement successElm;
    private int itemIdx;
    private List<ReceiptItemView> viewList = new ArrayList<>();
    private BiConsumer<List<ReceiptItem>, List<ReceiptItemView>> itemsSlot;

    public ReceiptViewTeaVM(ReceiptPresenter presenter) {
        super("receipt", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }

        if (this.state.notifySuccess) {
            setVisible(this.successElm, true);
            this.state.notifySuccess = false;
        }

        var items = Collections.<ReceiptItem>emptyList();
        var dateNewValue = this.dateOldValue;
        var totalNewValue = this.totalOldValue;
        if (this.state.receipt != null) {
            items = this.state.receipt.items;

            if (this.state.receipt.date != null) {
                dateNewValue = DateUtils.formatDateTime(this.state.receipt.date);
            } else {
                dateNewValue = "";
            }

            var total = this.state.receipt.total != null ? this.state.receipt.total : 0.0;
            totalNewValue = "R$ " + String.format("%.2f", total);
        }

        if (!Objects.equals(this.dateOldValue, dateNewValue)) {
            this.dateElm.setTextContent(dateNewValue);
            this.dateOldValue = dateNewValue;
        }

        if (!Objects.equals(this.totalOldValue, totalNewValue)) {
            this.totalElm.setTextContent(totalNewValue);
            this.totalOldValue = totalNewValue;
        }

        this.itemsSlot.accept(items, this.viewList);
    }

    private void buildUI(HtmlDom dom, HTMLElement root) {
        root.setAttribute("style", "max-width:900px;margin:0 auto;padding:12px");

        // Card container
        dom.div(null, card -> {
            card.setAttribute("style",
                    "background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:24px");

            dom.h5(null, title -> {
                title.setAttribute("style", "color:#666;font-size:0.85rem;margin:0 0 16px 0");
                title.setTextContent("IMPRIMA SEU RECIBO:");
            });

            // Success banner
            this.successElm = dom.div("d-none d-flex align-items-center gap-2 mb-3", success -> {
                success.setAttribute("style",
                        "background-color:#e8f5e9;border:1px solid #a5d6a7;border-radius:8px;padding:12px 16px;"
                        + "color:#2e7d32;font-size:1.2rem");
                dom.icon(BsIcons.CHECK_CIRCLE);
                dom.span(null, msg -> {
                    msg.setAttribute("style", "color:#2e7d32;font-weight:bold;font-size:1rem");
                    msg.setTextContent("Compra realizada com sucesso!");
                });
            });

            // Receipt content (monospace)
            dom.div(null, receiptBox -> {
                receiptBox.setAttribute("style",
                        "border:1px solid #bdbdbd;border-radius:8px;padding:16px;"
                        + "font-family:'Courier New',Courier,monospace;font-size:0.85rem");

                dom.span(null, caption1 -> {
                    caption1.setAttribute("style", "font-weight:600;display:block;margin-bottom:4px");
                    caption1.setTextContent("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET");
                });
                dom.span(null, caption2 -> {
                    caption2.setAttribute("style", "color:#666;display:block;margin-bottom:12px");
                    caption2.setTextContent("Recibo de compra");
                });

                dom.div("d-flex justify-content-between mb-2", dateRow -> {
                    dom.span(null, lbl -> {
                        lbl.setAttribute("style", "color:#666");
                        lbl.setTextContent("Data:");
                    });
                    this.dateElm = dom.span("fw-bold", date -> {});
                });
                dom.div("d-flex justify-content-between mb-3", totalRow -> {
                    dom.span(null, lbl -> {
                        lbl.setAttribute("style", "color:#666");
                        lbl.setTextContent("Total:");
                    });
                    this.totalElm = dom.span(null, total -> {
                        total.setAttribute("style", "font-weight:bold;color:#1976d2;font-size:1.1rem");
                    });
                });

                // Items table header
                dom.div(null, tableSection -> {
                    tableSection.setAttribute("style", "border-top:1px solid #e0e0e0;padding-top:8px");
                    dom.div("d-flex fw-bold small mb-1 pb-1", header -> {
                        header.setAttribute("style", "border-bottom:1px solid #e0e0e0;color:#666");
                        dom.span(null, h -> {
                            h.setAttribute("style", "flex:1");
                            h.setTextContent("ITEM");
                        });
                        dom.span(null, h -> {
                            h.setAttribute("style", "width:80px;text-align:center");
                            h.setTextContent("QTD");
                        });
                        dom.span(null, h -> {
                            h.setAttribute("style", "width:100px;text-align:right");
                            h.setTextContent("VALOR");
                        });
                    });

                    var itemsContainer = dom.div(null, items -> {});
                    this.itemsSlot = this.newListSlot(itemsContainer, this::newItemView, this::updateItem);
                });
            });

            // Back button
            dom.button("btn btn-link mt-3 p-0", backBtn -> {
                backBtn.setAttribute("style", "color:#1976d2;text-decoration:underline;font-size:0.85rem");
                dom.icon(BsIcons.ARROW_BACK);
                dom.span(null, txt -> txt.setTextContent(" Voltar aos produtos"));
                backBtn.addEventListener("click",
                        evt -> safeAction("Back", this.presenter::onOpenProducts));
            });
        });
    }

    private ReceiptItemView newItemView() {
        return new ReceiptItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ReceiptItemView itemView, ReceiptItem state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class: ReceiptItemView ----

    public static class ReceiptItemView extends AbstractViewTeaVM<ReceiptPresenter> {

        private ReceiptItem item;
        private String descOldValue;
        private int qtyOldValue;
        private String valueOldValue;
        private HTMLElement descElm;
        private HTMLElement qtyElm;
        private HTMLElement valueElm;

        ReceiptItemView(ShoppingTeaVMApplication app, ReceiptPresenter presenter, int idx) {
            super("receipt-item-" + idx, app, presenter,
                    HTMLDocument.current().createElement("div"));
            this.item = new ReceiptItem();
            this.element.setAttribute("style", "display:flex;align-items:center;padding:6px 0");
        }

        void setState(ReceiptItem item) {
            this.item = item;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                HtmlDom.render(this.element, this::buildUI);
                this.notRendered = false;
            }

            var desc = this.item.description != null ? this.item.description : "";
            if (!Objects.equals(this.descOldValue, desc)) {
                this.descElm.setTextContent(desc);
                this.descOldValue = desc;
            }

            if (this.qtyOldValue != this.item.quantity) {
                this.qtyElm.setTextContent(String.valueOf(this.item.quantity));
                this.qtyOldValue = this.item.quantity;
            }

            var value = "R$ " + String.format("%.2f", this.item.value);
            if (!Objects.equals(this.valueOldValue, value)) {
                this.valueElm.setTextContent(value);
                this.valueOldValue = value;
            }
        }

        private void buildUI(HtmlDom dom, HTMLElement row) {
            this.descElm = dom.span(null, desc -> {
                desc.setAttribute("style", "flex:1;font-size:0.85rem");
            });
            this.qtyElm = dom.span(null, qty -> {
                qty.setAttribute("style", "width:80px;text-align:center;font-size:0.85rem");
            });
            this.valueElm = dom.span(null, value -> {
                value.setAttribute("style", "width:100px;text-align:right;font-weight:bold;font-size:0.85rem");
            });
        }
    }
}
