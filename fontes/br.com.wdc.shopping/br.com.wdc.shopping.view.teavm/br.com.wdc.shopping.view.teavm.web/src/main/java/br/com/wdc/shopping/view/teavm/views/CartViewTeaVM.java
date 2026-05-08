package br.com.wdc.shopping.view.teavm.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class CartViewTeaVM extends AbstractViewTeaVM<CartPresenter> {

    private final CartViewState state;

    private int itemIdx;
    private List<CartItemView> cartItemViewList = new ArrayList<>();
    private BiConsumer<List<CartItem>, List<CartItemView>> itemsSlot;
    private HTMLElement totalCostElm;
    private double totalCostOldValue;
    private HTMLElement errorElm;
    private HTMLElement emptyPane;
    private HTMLElement contentPane;

    public CartViewTeaVM(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }

        this.itemsSlot.accept(this.state.items, this.cartItemViewList);

        var empty = this.state.items == null || this.state.items.isEmpty();
        setVisible(this.emptyPane, empty);
        setVisible(this.contentPane, !empty);

        var totalCostNewValue = computeTotalCost();
        if (totalCostNewValue != this.totalCostOldValue) {
            this.totalCostElm.setTextContent("R$ " + String.format("%.2f", totalCostNewValue));
            this.totalCostOldValue = totalCostNewValue;
        }

        var newErrorDisplay = false;
        var newErrorMessage = "";
        if (this.state.errorCode != 0) {
            newErrorDisplay = true;
            newErrorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        }
        if (!Objects.equals(this.errorElm.getTextContent(), newErrorMessage)) {
            this.errorElm.setTextContent(newErrorMessage);
        }
        setVisible(this.errorElm, newErrorDisplay);
    }

    private double computeTotalCost() {
        if (this.state.items == null) return 0;
        double total = 0;
        for (var item : this.state.items) {
            total += item.price * item.quantity;
        }
        return total;
    }

    private void buildUI(HtmlDom dom, HTMLElement root) {
        root.setAttribute("style", "max-width:900px;margin:0 auto;padding:12px");

        // Card container
        dom.div(null, card -> {
            card.setAttribute("style",
                    "background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:16px");

            // Title row
            dom.div("d-flex align-items-center gap-2 mb-3", titleBar -> {
                dom.icon(BsIcons.CART + " text-primary");
                dom.h5("mb-0 fw-bold", title -> {
                    title.setTextContent("Carrinho");
                });
            });

            dom.h6(null, subtitle -> {
                subtitle.setAttribute("style", "color:#666;font-size:0.85rem;margin:0 0 12px 0");
                subtitle.setTextContent("LISTA DE PRODUTOS");
            });

            // Error
            this.errorElm = dom.div("alert alert-danger d-none mb-3", err -> {});

            // Empty cart state
            this.emptyPane = dom.div("d-flex flex-column align-items-center justify-content-center py-5 d-none", empty -> {
                dom.div(null, iconDiv -> {
                    iconDiv.setAttribute("style",
                            "width:120px;height:120px;background-color:#e3f2fd;border-radius:50%;"
                            + "display:flex;align-items:center;justify-content:center;margin-bottom:16px;"
                            + "font-size:48px;color:#1976d2");
                    dom.icon("bi bi-cart3");
                });
                dom.p(null, msg -> {
                    msg.setAttribute("style", "color:#666;font-size:1.1rem");
                    msg.setTextContent("Seu carrinho está vazio");
                });
                dom.span(null, link -> {
                    link.setAttribute("style", "cursor:pointer;color:#1976d2;font-weight:bold;font-size:1rem");
                    link.setTextContent("Vamos às compras!");
                    link.addEventListener("click",
                            evt -> safeAction("Go shopping", this.presenter::onOpenProducts));
                });
            });

            // Cart content
            this.contentPane = dom.div("d-none", content -> {
                var itemsContainer = dom.div(null, items -> {});
                this.itemsSlot = this.newListSlot(itemsContainer, this::newItemView, this::updateItem);

                // Footer
                dom.div("d-flex justify-content-end align-items-center pt-3 mt-3", footer -> {
                    footer.setAttribute("style", "border-top:1px solid #e0e0e0");
                    dom.span("fw-bold", totalLabel -> {
                        totalLabel.setTextContent("VALOR TOTAL: ");
                    });

                    this.totalCostElm = dom.span(null, total -> {
                        total.setAttribute("style", "font-size:18px;font-weight:bold;color:#1976d2;margin-left:8px");
                        total.setTextContent("R$ 0,00");
                    });
                });

                // Actions
                dom.div("d-flex justify-content-between align-items-center mt-3", actions -> {
                    dom.button("btn btn-link p-0", backBtn -> {
                        backBtn.setAttribute("style", "color:#1976d2;text-decoration:underline;font-size:0.85rem");
                        dom.icon(BsIcons.ARROW_BACK);
                        dom.span(null, txt -> txt.setTextContent(" Voltar aos produtos"));
                        backBtn.addEventListener("click",
                                evt -> safeAction("Back", this.presenter::onOpenProducts));
                    });

                    dom.button("btn btn-success", buyBtn -> {
                        dom.icon(BsIcons.BAG);
                        dom.span(null, txt -> txt.setTextContent(" FINALIZAR PEDIDO"));
                        buyBtn.addEventListener("click",
                                evt -> safeAction("Buy", this.presenter::onBuy));
                    });
                });
            });
        });
    }

    private CartItemView newItemView() {
        return new CartItemView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(CartItemView itemView, CartItem state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class: CartItemView ----

    public static class CartItemView extends AbstractViewTeaVM<CartPresenter> {

        private CartItem item;
        private String nameOldValue;
        private String subtotalOldValue;
        private int quantityOldValue;
        private HTMLElement nameElm;
        private HTMLElement quantityElm;
        private HTMLElement subtotalElm;

        CartItemView(ShoppingTeaVMApplication app, CartPresenter presenter, int idx) {
            super("cart-item-" + idx, app, presenter,
                    HTMLDocument.current().createElement("div"));
            this.item = new CartItem();
            this.element.setAttribute("style",
                    "display:flex;align-items:center;padding:10px 0;border-bottom:1px solid #f0f0f0");
        }

        void setState(CartItem item) {
            this.item = item;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                HtmlDom.render(this.element, this::buildUI);
                this.notRendered = false;
            }

            var name = this.item.name != null ? this.item.name : "";
            if (!Objects.equals(this.nameOldValue, name)) {
                this.nameElm.setTextContent(name);
                this.nameOldValue = name;
            }

            if (this.quantityOldValue != this.item.quantity) {
                this.quantityElm.setTextContent("x" + this.item.quantity);
                this.quantityOldValue = this.item.quantity;
            }

            var subtotal = "R$ " + String.format("%.2f", this.item.price * this.item.quantity);
            if (!Objects.equals(this.subtotalOldValue, subtotal)) {
                this.subtotalElm.setTextContent(subtotal);
                this.subtotalOldValue = subtotal;
            }
        }

        private void buildUI(HtmlDom dom, HTMLElement row) {
            // Item name (flex grow)
            this.nameElm = dom.span(null, name -> {
                name.setAttribute("style", "flex:1;font-weight:500;font-size:0.9rem");
            });
            // Price
            this.subtotalElm = dom.span(null, subtotal -> {
                subtotal.setAttribute("style", "width:100px;text-align:right;font-weight:bold;color:#1976d2;font-size:0.9rem");
            });
            // Quantity
            this.quantityElm = dom.span(null, qty -> {
                qty.setAttribute("style", "width:50px;text-align:center;font-size:0.85rem;color:#666");
            });
            // Delete button
            dom.button("btn btn-sm btn-outline-danger ms-2", removeBtn -> {
                dom.icon(BsIcons.TRASH);
                removeBtn.addEventListener("click",
                        evt -> safeAction("Remove item", () -> this.presenter.onRemoveProduct(this.item.id)));
            });
        }
    }
}
