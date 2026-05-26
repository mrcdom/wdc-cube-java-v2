package br.com.wdc.shopping.view.teavm.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class ProductsPanelViewTeaVM extends AbstractViewTeaVM<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    private int itemIdx;
    private List<ProductCardView> itemViewList = new ArrayList<>();
    private BiConsumer<List<ProductInfo>, List<ProductCardView>> contentSlot;

    public ProductsPanelViewTeaVM(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
        this.element.getClassList().add("p-3");
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }
        this.contentSlot.accept(this.state.products, this.itemViewList);
    }

    private void buildUI(HtmlDom dom, HTMLElement root) {
        dom.h6("fw-bold text-uppercase mb-2", caption -> {
            caption.setAttribute("style", "color:#666;font-size:0.85rem");
            caption.setTextContent("PRODUTOS");
        });

        var listContainer = dom.div("row row-cols-2 row-cols-md-3 g-3", container -> {
        });
        this.contentSlot = this.newListSlot(listContainer, this::newItemView, this::updateItem);
    }

    private ProductCardView newItemView() {
        return new ProductCardView(this.app, this.presenter, this.itemIdx++);
    }

    private void updateItem(ProductCardView itemView, ProductInfo state) {
        itemView.setState(state);
        itemView.doUpdate();
    }

    // ---- Inner class: ProductCardView ----

    public static class ProductCardView extends AbstractViewTeaVM<ProductsPanelPresenter> {

        private static final String CARD_STYLE = "background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;"
                + "cursor:pointer;transition:all 0.25s cubic-bezier(0.4,0,0.2,1);overflow:hidden";
        private static final String CARD_HOVER_STYLE = "background-color:#fff;border-radius:12px;border:1px solid #64b5f6;"
                + "cursor:pointer;transition:all 0.25s cubic-bezier(0.4,0,0.2,1);overflow:hidden;"
                + "transform:translateY(-4px);box-shadow:0 12px 24px rgba(0,0,0,0.1)";

        private ProductInfo product;
        private String oldProductName;
        private String oldProductPrice;
        private String oldProductImage;
        private HTMLImageElement imageElm;
        private HTMLElement nameElm;
        private HTMLElement priceElm;

        ProductCardView(ShoppingTeaVMApplication app, ProductsPanelPresenter presenter, int idx) {
            super("product-card-" + idx, app, presenter,
                    HTMLDocument.current().createElement("div"));
            this.product = new ProductInfo();
            this.element.getClassList().add("col");
        }

        void setState(ProductInfo product) {
            this.product = product;
        }

        @Override
        public void doUpdate() {
            if (this.notRendered) {
                HtmlDom.render(this.element, this::buildUI);
                this.notRendered = false;
            }

            var newImage = this.product.image != null ? app.resolveImageUrl(this.product.image) : "";
            if (!Objects.equals(this.oldProductImage, newImage) && !newImage.isEmpty()) {
                this.imageElm.setSrc(newImage);
                this.oldProductImage = newImage;
            }

            var newProductName = this.product.name != null ? this.product.name : "";
            if (!Objects.equals(this.oldProductName, newProductName)) {
                this.nameElm.setTextContent(newProductName);
                this.oldProductName = newProductName;
            }

            var newProductPrice = this.product.price > 0
                    ? "R$ " + String.format("%.2f", this.product.price)
                    : "";
            if (!Objects.equals(this.oldProductPrice, newProductPrice)) {
                this.priceElm.setTextContent(newProductPrice);
                this.oldProductPrice = newProductPrice;
            }
        }

        private void buildUI(HtmlDom dom, HTMLElement root) {
            dom.div(null, card -> {
                card.setAttribute("style", CARD_STYLE);
                final boolean[] touchActive = { false };
                card.addEventListener("mouseenter", evt -> {
                    if (!touchActive[0])
                        card.setAttribute("style", CARD_HOVER_STYLE);
                });
                card.addEventListener("mouseleave", evt -> card.setAttribute("style", CARD_STYLE));
                card.addEventListener("touchstart", evt -> {
                    touchActive[0] = true;
                    card.setAttribute("style", CARD_HOVER_STYLE);
                });
                card.addEventListener("touchend", evt -> {
                    card.setAttribute("style", CARD_STYLE);
                    Window.setTimeout(() -> touchActive[0] = false, 300);
                });
                card.addEventListener("touchcancel", evt -> {
                    card.setAttribute("style", CARD_STYLE);
                    Window.setTimeout(() -> touchActive[0] = false, 300);
                });
                card.addEventListener("click",
                        evt -> safeAction("Open product", () -> this.presenter.onOpenProduct(this.product.id)));

                // Image pane
                dom.div("d-flex align-items-center justify-content-center", imagePane -> {
                    imagePane.setAttribute("style",
                            "background-color:#f8f9fa;padding:16px;border-bottom:1px solid #f0f0f0");
                    this.imageElm = dom.img(null, img -> {
                        img.setAlt("Produto");
                        img.setAttribute("style", "width:100px;height:100px;object-fit:contain");
                    });
                });

                // Label group
                dom.div(null, labelGroup -> {
                    labelGroup.setAttribute("style", "padding:16px");
                    this.nameElm = dom.p(null, name -> {
                        name.setAttribute("style", "font-size:0.85rem;font-weight:500;margin:0 0 8px 0;color:#333");
                    });
                    this.priceElm = dom.span(null, price -> {
                        price.setAttribute("style", "font-size:1.1rem;font-weight:bold;color:#1976d2");
                    });
                });
            });
        }
    }
}
