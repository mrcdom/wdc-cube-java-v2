package br.com.wdc.shopping.view.remote.shell.teavm;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewStateCoordinator;
import br.com.wdc.shopping.view.teavm.commons.interop.Console;
import br.com.wdc.shopping.view.remote.shell.teavm.views.BrowserView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.CartView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.HomeView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.LoginView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.ProductView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.ProductsPanelView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.PurchasesPanelView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.ReceiptView;
import br.com.wdc.shopping.view.remote.shell.teavm.views.RootView;

/**
 * Entry point da aplicação remote shell TeaVM.
 * Conecta via WebSocket ao remote.host e renderiza com VDom.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Console.log("WDC Shopping Remote Shell TeaVM - Initializing...");

            // Register view factories
            ViewStateCoordinator app = ViewStateCoordinator.INSTANCE;
            app.registerView(BrowserView.VIEW_ID, BrowserView::new);
            app.registerView(RootView.VIEW_ID, RootView::new);
            app.registerView(LoginView.VIEW_ID, LoginView::new);
            app.registerView(HomeView.VIEW_ID, HomeView::new);
            app.registerView(CartView.VIEW_ID, CartView::new);
            app.registerView(ProductView.VIEW_ID, ProductView::new);
            app.registerView(ReceiptView.VIEW_ID, ReceiptView::new);
            app.registerView(ProductsPanelView.VIEW_ID, ProductsPanelView::new);
            app.registerView(PurchasesPanelView.VIEW_ID, PurchasesPanelView::new);

            // Start
            app.start();
            // Loading screen is removed by BrowserView on first render

            Console.log("WDC Shopping Remote Shell TeaVM - Started.");
        } catch (Exception e) {
            Console.error("FATAL: " + e.getClass().getName() + ": " + e.getMessage());
            showError(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @org.teavm.jso.JSBody(params = {"msg"}, script = ""
            + "var el = document.getElementById('loading');"
            + "if (el) { el.style.color='red'; el.textContent = 'ERROR: ' + msg; }")
    private static native void showError(String msg);

}
