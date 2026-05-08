package br.com.wdc.shopping.view.teavm.views;

import java.util.Objects;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class HomeViewTeaVM extends AbstractViewTeaVM<HomePresenter> {

    private final HomeViewState state;

    private HTMLElement nickNameElm;
    private String nickNameOldValue;
    private HTMLElement cartCountElm;
    private int cartCountOldValue;
    private HTMLElement errorElm;
    private HTMLElement contentPane;
    private HTMLElement defaultContentPane;
    private HTMLElement productsPanelSlot;
    private HTMLElement purchasesPanelSlot;
    private AbstractViewTeaVM<?> currentContentView;
    private HTMLElement tabProductsBtn;
    private HTMLElement tabPurchasesBtn;
    private boolean showingProducts = true;

    public HomeViewTeaVM(HomePresenter presenter) {
        super("home", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "flex-column", "h-100");
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
        }

        if (!Objects.equals(this.nickNameOldValue, this.state.nickName)) {
            this.nickNameElm.setTextContent(this.state.nickName);
            this.nickNameOldValue = this.state.nickName;
        }

        if (this.cartCountOldValue != this.state.cartItemCount) {
            this.cartCountElm.setTextContent(String.valueOf(this.state.cartItemCount));
            this.cartCountOldValue = this.state.cartItemCount;
        }

        if (this.state.productsPanelView instanceof AbstractViewTeaVM<?> ppv
                && ppv.getElement().getParentNode() != this.productsPanelSlot) {
            this.productsPanelSlot.clear();
            this.productsPanelSlot.appendChild(ppv.getElement());
        }

        if (this.state.purchasesPanelView instanceof AbstractViewTeaVM<?> ppv
                && ppv.getElement().getParentNode() != this.purchasesPanelSlot) {
            this.purchasesPanelSlot.clear();
            this.purchasesPanelSlot.appendChild(ppv.getElement());
        }

        var newContentView = this.state.contentView instanceof AbstractViewTeaVM<?> v ? v : null;
        if (this.currentContentView != newContentView) {
            this.contentPane.clear();
            if (newContentView != null) {
                this.contentPane.appendChild(newContentView.getElement());
            } else {
                this.contentPane.appendChild(this.defaultContentPane);
            }
            this.currentContentView = newContentView;
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
        // App bar (navbar) — 3-section layout: left (exit + greeting), center (logo), right (cart)
        dom.nav("navbar navbar-dark px-2 px-sm-3", appBar -> {
            appBar.setAttribute("style",
                    "background-color:#1976d2;min-height:56px;box-shadow:0 2px 4px rgba(0,0,0,0.1);"
                    + "display:flex;flex-wrap:nowrap;align-items:center;justify-content:space-between");

            // Left section: exit button + greeting
            dom.div("d-flex align-items-center gap-1 gap-sm-2", userBox -> {
                dom.button(null, exitBtn -> {
                    exitBtn.setAttribute("style",
                            "background:none;border:none;color:rgba(255,255,255,0.9);font-size:1.25rem;"
                            + "cursor:pointer;padding:4px 6px;display:flex;align-items:center");
                    dom.icon(BsIcons.POWER);
                    exitBtn.addEventListener("click",
                            evt -> safeAction("Exit", this.presenter::onExit));
                });

                dom.span("d-none d-sm-inline", greeting -> {
                    greeting.setAttribute("style", "color:rgba(255,255,255,0.85);font-size:0.9rem");
                    greeting.setTextContent("Olá,");
                });
                this.nickNameElm = dom.span("d-none d-sm-inline", nick -> {
                    nick.setAttribute("style", "color:#fff;font-weight:600;font-size:0.9rem");
                    nick.setTextContent(this.state.nickName);
                });
                this.nickNameOldValue = this.state.nickName;
            });

            // Center section: logo
            dom.div("d-flex align-items-center gap-1 gap-sm-2", logoBox -> {
                // Price tag icon (matching login header style)
                dom.span("bi bi-cart3", icon -> {
                    icon.setAttribute("style", "color:#ff9800;font-size:1.25rem");
                });
                dom.div(null, titleBox -> {
                    dom.span(null, t -> {
                        t.setAttribute("style",
                                "color:#fff;font-size:1.1rem;font-weight:700;letter-spacing:0.5px;display:block;line-height:1.2");
                        t.setTextContent("Shopping");
                    });
                    dom.div("d-none d-sm-block", sub -> {
                        sub.setAttribute("style",
                                "color:rgba(255,255,255,0.55);font-size:0.6rem;letter-spacing:0.3px");
                        sub.setTextContent("by WeDoCode");
                    });
                });
            });

            // Right section: cart button with badge
            dom.div("d-flex align-items-center", actionBox -> {
                dom.button(null, cartBtn -> {
                    cartBtn.setAttribute("style",
                            "background:none;border:none;color:rgba(255,255,255,0.9);cursor:pointer;"
                            + "padding:4px 8px;display:flex;align-items:center;gap:6px;position:relative");
                    dom.icon(BsIcons.CART);
                    dom.span("d-none d-sm-inline", label -> {
                        label.setAttribute("style", "font-size:0.9rem;color:#fff");
                        label.setTextContent("Carrinho");
                    });
                    this.cartCountElm = dom.span(null, badge -> {
                        badge.setAttribute("style",
                                "position:absolute;top:-2px;right:-6px;font-size:10px;min-width:18px;"
                                + "text-align:center;background-color:#ff9800;color:white;border-radius:50%;padding:2px 5px");
                        badge.setTextContent(String.valueOf(this.state.cartItemCount));
                    });
                    this.cartCountOldValue = this.state.cartItemCount;
                    cartBtn.addEventListener("click",
                            evt -> safeAction("Open cart", this.presenter::onOpenCart));
                });
            });
        });

        // Error label
        this.errorElm = dom.div("alert alert-danger m-2 d-none", err -> {});

        // Content pane
        this.contentPane = dom.div("flex-grow-1 overflow-auto", cp -> {
            cp.setAttribute("style", "background-color:#ededed");
            this.defaultContentPane = dom.div("h-100", dp -> {
                this.productsPanelSlot = dom.div(null, slot -> {});
                this.purchasesPanelSlot = dom.div("d-none h-100", slot -> {});
            });
        });

        // Bottom navigation
        dom.footer("d-flex border-top bg-white", bottomNav -> {
            this.tabProductsBtn = dom.button("btn flex-grow-1 rounded-0 py-3 fw-bold text-primary border-0", btn -> {
                dom.icon(BsIcons.SHOP);
                dom.span(null, txt -> txt.setTextContent(" Produtos"));
                btn.addEventListener("click", evt -> switchTab(true));
            });

            this.tabPurchasesBtn = dom.button("btn flex-grow-1 rounded-0 py-3 text-muted border-0", btn -> {
                dom.icon(BsIcons.CLOCK_HISTORY);
                dom.span(null, txt -> txt.setTextContent(" Histórico"));
                btn.addEventListener("click", evt -> switchTab(false));
            });
        });
    }

    private void switchTab(boolean showProducts) {
        if (this.currentContentView != null) {
            safeAction("Back to home", () -> Routes.home(this.app));
        }
        this.showingProducts = showProducts;
        setVisible(this.productsPanelSlot, showProducts);
        setVisible(this.purchasesPanelSlot, !showProducts);
        updateTabStyles();
        // Trigger re-measurement when the purchases panel becomes visible
        if (!showProducts && this.state.purchasesPanelView != null) {
            this.state.purchasesPanelView.update();
        }
    }

    private void updateTabStyles() {
        var productsCl = this.tabProductsBtn.getClassList();
        var purchasesCl = this.tabPurchasesBtn.getClassList();
        if (this.showingProducts) {
            productsCl.remove("text-muted");
            productsCl.add("text-primary", "fw-bold");
            purchasesCl.remove("text-primary", "fw-bold");
            purchasesCl.add("text-muted");
        } else {
            purchasesCl.remove("text-muted");
            purchasesCl.add("text-primary", "fw-bold");
            productsCl.remove("text-primary", "fw-bold");
            productsCl.add("text-muted");
        }
    }
}
