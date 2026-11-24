package br.com.wdc.store.view.remote.shell.teavm;

import br.com.wdc.framework.cube.remote.bridge.teavm.EncryptedLocalStorage;
import br.com.wdc.framework.cube.remote.bridge.teavm.SecurityBoot;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewStateCoordinator;
import br.com.wdc.store.view.teavm.commons.interop.Console;
import br.com.wdc.store.view.remote.shell.teavm.views.RootView;
import br.com.wdc.store.view.remote.shell.teavm.views.browser.BrowserView;
import br.com.wdc.store.view.remote.shell.teavm.views.cart.CartItemView;
import br.com.wdc.store.view.remote.shell.teavm.views.cart.CartView;
import br.com.wdc.store.view.remote.shell.teavm.views.home.HomeView;
import br.com.wdc.store.view.remote.shell.teavm.views.home.ProductsPanelView;
import br.com.wdc.store.view.remote.shell.teavm.views.home.PurchasesPanelView;
import br.com.wdc.store.view.remote.shell.teavm.views.login.LoginView;
import br.com.wdc.store.view.remote.shell.teavm.views.product.ProductView;
import br.com.wdc.store.view.remote.shell.teavm.views.receipt.ReceiptView;

/**
 * Entry point of the TeaVM remote shell application.
 * Connects to remote.host over WebSocket and renders with VDom.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Console.log("WDC Store Remote Shell TeaVM - Initializing...");

            // Configure sync namespace — must be before ViewStateCoordinator.INSTANCE is used
            ViewStateCoordinator.configure("~rt:");

            // Security boot (RSA + AES key derivation) — starts async PBKDF2
            SecurityBoot.initialize();

            // Register view factories
            ViewStateCoordinator app = ViewStateCoordinator.INSTANCE;
            app.registerView(BrowserView.VIEW_ID, BrowserView::new);
            app.registerView(RootView.VIEW_ID, RootView::new);
            app.registerView(LoginView.VIEW_ID, LoginView::new);
            app.registerView(HomeView.VIEW_ID, HomeView::new);
            app.registerView(CartView.VIEW_ID, CartView::new);
            app.registerView(CartItemView.VIEW_ID, CartItemView::new);
            app.registerView(ProductView.VIEW_ID, ProductView::new);
            app.registerView(ReceiptView.VIEW_ID, ReceiptView::new);
            app.registerView(ProductsPanelView.VIEW_ID, ProductsPanelView::new);
            app.registerView(PurchasesPanelView.VIEW_ID, PurchasesPanelView::new);

            // Open IndexedDB key + decrypt sec.* cache, then start WebSocket.
            // SecurityBoot PBKDF2 runs in parallel; FlushRequestContext.open()
            // already waits for SecurityBoot.onReady() before sending the first
            // message, so the two async paths are correctly sequenced.
            EncryptedLocalStorage.initialize(() -> {
                app.start();
                // Loading screen is removed by BrowserView on first render
                Console.log("WDC Store Remote Shell TeaVM - Started.");
            });

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
