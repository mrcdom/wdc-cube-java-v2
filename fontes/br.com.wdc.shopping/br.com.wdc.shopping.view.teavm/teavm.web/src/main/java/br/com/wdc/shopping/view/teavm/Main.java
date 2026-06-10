package br.com.wdc.shopping.view.teavm;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.interop.Console;
import br.com.wdc.shopping.view.teavm.infra.EncryptedLocalStorage;

/**
 * Entry point para a aplicação TeaVM.
 * Chamado após o carregamento do JavaScript gerado pelo TeaVM.
 */
public class Main {

    public static void main(String[] args) {
        try {
            Console.log("WDC Shopping TeaVM - Initializing...");

            // URL base da API - pode ser configurada via atributo no HTML
            String apiBaseUrl = getApiBaseUrl();
            Console.log("API Base URL: " + apiBaseUrl);

            // Cria a aplicação e inicializa o storage seguro (IndexedDB + AES-GCM)
            // antes de iniciar o app, para que tryRestore() encontre os tokens cifrados.
            ShoppingTeaVMApplication app = new ShoppingTeaVMApplication(apiBaseUrl);
            Console.log("WDC Shopping TeaVM - App created, initializing secure storage...");
            EncryptedLocalStorage.configure("tw");
            EncryptedLocalStorage.initialize(() -> {
                app.start();
                removeLoadingScreen();
                LoginPresenter.simulateSlowLogin(false);
                Console.log("WDC Shopping TeaVM - Started.");
            });
        } catch (Exception e) {
            Console.error("WDC Shopping TeaVM - FATAL: " + e.getClass().getName() + ": " + e.getMessage());
            showError(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    @org.teavm.jso.JSBody(params = {"msg"}, script = ""
            + "var el = document.getElementById('loading');"
            + "if (el) { el.style.color='red'; el.textContent = 'ERROR: ' + msg; }")
    private static native void showError(String msg);

    @org.teavm.jso.JSBody(params = {}, script = ""
            + "var el = document.getElementById('loading');"
            + "if (el) el.remove();")
    private static native void removeLoadingScreen();

    @org.teavm.jso.JSBody(params = {}, script = ""
            + "var meta = document.querySelector('meta[name=\"api-base-url\"]');"
            + "return (meta && meta.content) ? meta.content : '';")
    private static native String getApiBaseUrl();

}
