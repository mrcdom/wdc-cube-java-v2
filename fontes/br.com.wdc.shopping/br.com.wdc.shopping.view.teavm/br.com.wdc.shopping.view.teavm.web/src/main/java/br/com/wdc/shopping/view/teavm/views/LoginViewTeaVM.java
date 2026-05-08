package br.com.wdc.shopping.view.teavm.views;

import java.util.Objects;

import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.HtmlDom;

public class LoginViewTeaVM extends AbstractViewTeaVM<LoginPresenter> {

    private final LoginViewState state;
    private HTMLInputElement userNameField;
    private HTMLInputElement passwordField;
    private HTMLElement errorElm;

    public LoginViewTeaVM(LoginPresenter presenter) {
        super("login", (ShoppingTeaVMApplication) presenter.app, presenter,
                HTMLDocument.current().createElement("div"));
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "justify-content-center", "align-items-center", "vh-100");
        this.element.getStyle().setCssText("background:#f5f5f5");
    }

    @Override
    public void doUpdate() {
        if (this.notRendered) {
            HtmlDom.render(this.element, this::buildUI);
            this.notRendered = false;
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
        dom.div("card shadow mx-auto", card -> {
            card.setAttribute("style",
                    "max-width:440px;width:100%;border-radius:16px;border:none;overflow:hidden");

            // Blue header with logo
            dom.div("", header -> {
                header.setAttribute("style",
                        "background:#4285f4;padding:1.5rem 2rem;display:flex;align-items:center;gap:0.75rem");
                dom.span("bi bi-cart3", icon -> {
                    icon.setAttribute("style", "color:#ff9800;font-size:1.75rem");
                });
                dom.div("", titleBox -> {
                    dom.span("", t -> {
                        t.setAttribute("style",
                                "color:#fff;font-size:1.5rem;font-weight:700;letter-spacing:0.5px");
                        t.setTextContent("Shopping");
                    });
                    dom.div("", sub -> {
                        sub.setAttribute("style", "color:rgba(255,255,255,0.65);font-size:0.75rem");
                        sub.setTextContent("by WeDoCode");
                    });
                });
            });

            dom.div("", body -> {
                body.setAttribute("style", "padding:2rem 2rem 1.5rem");

                // Lock icon
                dom.div("text-center mb-3", iconBox -> {
                    dom.span("bi bi-lock-fill", icon -> {
                        icon.setAttribute("style",
                                "display:inline-flex;align-items:center;justify-content:center;"
                                + "width:48px;height:48px;border-radius:50%;background:#e8eaf6;"
                                + "color:#1976d2;font-size:1.5rem");
                    });
                });

                // Title
                dom.h5("text-center fw-bold mb-4", title -> {
                    title.setAttribute("style", "color:#333;font-size:1.25rem");
                    title.setTextContent("Acesso ao sistema");
                });

                // Error alert
                this.errorElm = dom.div("alert alert-danger d-none", err -> {});

                // Username
                dom.div("mb-3", group -> {
                    this.userNameField = dom.input("text", "form-control form-control-lg", field -> {
                        field.setAttribute("placeholder", "Usuário");
                        field.setAttribute("autocomplete", "off");
                        field.setAttribute("data-form-type", "other");
                        field.setAttribute("style",
                                "border-radius:8px;border:1px solid #ccc;font-size:1rem");
                    });
                });

                // Password
                dom.div("mb-4", group -> {
                    this.passwordField = dom.input("password", "form-control form-control-lg", field -> {
                        field.setAttribute("placeholder", "Senha");
                        field.setAttribute("autocomplete", "off");
                        field.setAttribute("data-form-type", "other");
                        field.setAttribute("style",
                                "border-radius:8px;border:1px solid #ccc;font-size:1rem");
                    });
                    this.passwordField.addEventListener("keydown", (KeyboardEvent evt) -> {
                        if ("Enter".equals(evt.getKey())) {
                            emitEnter();
                        }
                    });
                });

                // Login button
                dom.button("btn btn-primary w-100 fw-bold", btn -> {
                    btn.setAttribute("style",
                            "border-radius:8px;padding:0.75rem;font-size:1.1rem;"
                            + "background:#4285f4;border:none");
                    btn.setTextContent("Entrar");
                    btn.addEventListener("click", evt -> emitEnter());
                });

                // Separator
                dom.div("", hr -> {
                    hr.setAttribute("style",
                            "margin:1.5rem 0 1rem;border-top:1px solid #e0e0e0");
                });

                // Demo hint
                dom.div("text-center", demoBox -> {
                    demoBox.setAttribute("style",
                            "border:1px dashed #ccc;border-radius:8px;padding:0.6rem 1rem;"
                            + "color:#888;font-size:0.85rem");
                    dom.span("", txt -> {
                        txt.setTextContent("Acesso demo: usuário ");
                    });
                    dom.span("fw-bold", bold1 -> {
                        bold1.setAttribute("style", "color:#555");
                        bold1.setTextContent("admin");
                    });
                    dom.span("", txt2 -> {
                        txt2.setTextContent(" / senha ");
                    });
                    dom.span("fw-bold", bold2 -> {
                        bold2.setAttribute("style", "color:#555");
                        bold2.setTextContent("admin");
                    });
                });
            });
        });
    }

    private void emitEnter() {
        safeAction("Login", () -> {
            this.state.userName = this.userNameField.getValue();
            this.state.password = this.passwordField.getValue();
            this.presenter.onEnter();
        });
    }
}
