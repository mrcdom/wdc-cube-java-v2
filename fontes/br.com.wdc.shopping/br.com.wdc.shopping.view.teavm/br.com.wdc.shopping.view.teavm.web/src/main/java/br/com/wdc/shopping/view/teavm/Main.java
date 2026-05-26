package br.com.wdc.shopping.view.teavm;

import br.com.wdc.shopping.view.teavm.interop.Console;

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

            // Cria e inicia a aplicação
            ShoppingTeaVMApplication app = new ShoppingTeaVMApplication(apiBaseUrl);
            Console.log("WDC Shopping TeaVM - App created, starting...");
            app.start();
            Console.log("WDC Shopping TeaVM - App started, removing loading...");

            // Remove tela de loading
            removeLoadingScreen();

            Console.log("WDC Shopping TeaVM - Started.");
        } catch (Throwable e) {
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
            + "var path = window.location.pathname;"
            + "var idx = path.indexOf('/', 1);"
            + "return idx > 0 ? path.substring(0, idx) : '';")
    private static native String getApiBaseUrl();

}
