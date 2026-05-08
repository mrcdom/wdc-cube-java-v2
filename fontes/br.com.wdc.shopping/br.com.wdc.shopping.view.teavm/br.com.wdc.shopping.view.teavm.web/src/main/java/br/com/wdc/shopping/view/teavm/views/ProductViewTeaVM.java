package br.com.wdc.shopping.view.teavm.views;

import java.util.Objects;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLImageElement;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class ProductViewTeaVM extends AbstractViewTeaVM<ProductPresenter> {

    private final ProductViewState state;

    private HTMLImageElement imageElm;
    private String imageOldValue;
    private HTMLElement nameElm;
    private String nameOldValue;
    private HTMLElement priceElm;
    private double priceOldValue;
    private HTMLElement descriptionElm;
    private String descriptionOldValue;
    private HTMLElement errorElm;
    private int quantity = 1;
    private HTMLElement quantityLabel;

    public ProductViewTeaVM(ProductPresenter presenter) {
        super("product", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }

        if (this.state.product != null) {
            var newImage = this.state.product.image != null ? app.resolveImageUrl(this.state.product.image) : "";
            if (!Objects.equals(this.imageOldValue, newImage) && !newImage.isEmpty()) {
                this.imageElm.setSrc(newImage);
                this.imageOldValue = newImage;
            }

            if (!Objects.equals(this.nameOldValue, this.state.product.name)) {
                this.nameElm.setTextContent(this.state.product.name);
                this.nameOldValue = this.state.product.name;
            }

            if (this.priceOldValue != this.state.product.price) {
                this.priceElm.setTextContent("R$ " + String.format("%.2f", this.state.product.price));
                this.priceOldValue = this.state.product.price;
            }

            var desc = this.state.product.description != null ? this.state.product.description : "";
            if (!Objects.equals(this.descriptionOldValue, desc)) {
                this.descriptionElm.setInnerHTML(desc);
                this.descriptionOldValue = desc;
            }
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

    private void buildUI(HtmlDom dom, HTMLElement root) {
        root.setAttribute("style", "max-width:900px;margin:0 auto;padding:12px");

        // Card container
        dom.div(null, card -> {
            card.setAttribute("style",
                    "background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:16px");

            // Product name
            this.nameElm = dom.h5(null, name -> {
                name.setAttribute("style", "font-weight:bold;margin:0 0 16px 0");
            });

            // Info row: stacks on mobile, side-by-side on sm+
            dom.div("d-flex flex-column-reverse flex-sm-row align-items-center gap-3 mb-3", infoRow -> {
                // Left column
                dom.div("d-flex flex-column w-100", left -> {
                    this.priceElm = dom.p(null, price -> {
                        price.setAttribute("style", "font-size:1.5rem;font-weight:bold;color:#1976d2;margin:0 0 12px 0");
                    });

                    // Quantity stepper
                    dom.div("d-flex align-items-center gap-2 mb-3", stepper -> {
                        dom.span(null, lbl -> lbl.setTextContent("Quantidade:"));

                        dom.button("btn btn-sm btn-outline-secondary", minusBtn -> {
                            dom.icon(BsIcons.DASH);
                            minusBtn.addEventListener("click", evt -> {
                                if (this.quantity > 1) {
                                    this.quantity--;
                                    this.quantityLabel.setTextContent(String.valueOf(this.quantity));
                                }
                            });
                        });

                        this.quantityLabel = dom.span("fw-bold", qty -> {
                            qty.setTextContent("1");
                        });

                        dom.button("btn btn-sm btn-outline-secondary", plusBtn -> {
                            dom.icon(BsIcons.PLUS);
                            plusBtn.addEventListener("click", evt -> {
                                this.quantity++;
                                this.quantityLabel.setTextContent(String.valueOf(this.quantity));
                            });
                        });
                    });

                    // Add to cart button
                    dom.button("btn btn-primary", addBtn -> {
                        dom.icon(BsIcons.CART);
                        dom.span(null, txt -> txt.setTextContent(" Adicionar"));
                        addBtn.addEventListener("click",
                                evt -> safeAction("AddToCart", () -> this.presenter.onAddToCart(this.quantity)));
                    });
                });

                // Right: product image
                dom.div("text-center", right -> {
                    this.imageElm = dom.img(null, img -> {
                        img.setAttribute("alt", "Produto");
                        img.setAttribute("style", "max-width:200px;max-height:200px;width:100%;height:auto;object-fit:contain");
                    });
                });
            });

            // Description label
            dom.span(null, descLabel -> {
                descLabel.setAttribute("style", "color:#666;font-weight:600;font-size:0.85rem");
                descLabel.setTextContent("Descrição");
            });

            // Description content
            this.descriptionElm = dom.div(null, desc -> {
                desc.setAttribute("style", "font-size:0.85rem;line-height:1.5;margin-top:4px");
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

        // Error
        this.errorElm = dom.div("alert alert-danger m-3 d-none", err -> {});
    }
}
