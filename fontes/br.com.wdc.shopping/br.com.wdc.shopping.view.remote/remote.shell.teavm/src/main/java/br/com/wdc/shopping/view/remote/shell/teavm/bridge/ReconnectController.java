package br.com.wdc.shopping.view.remote.shell.teavm.bridge;

import br.com.wdc.shopping.view.teavm.commons.interop.Timers;

/**
 * Controla reconexão com backoff exponencial.
 * Equivalente ao ReconnectController.ts do remote.shell.react.
 */
public class ReconnectController {

    private final ViewStateCoordinator app;

    String url = "";
    int count = 0;
    int reconnectHandler = 0;
    int delay = 0;
    String cause = "";

    public ReconnectController(ViewStateCoordinator app) {
        this.app = app;
        this.url = app.getBaseWebSocketUrl() + "/dispatcher/" + app.id;
    }

    public void close() {
        if (reconnectHandler != 0) {
            Timers.clearInterval(reconnectHandler);
            reconnectHandler = 0;
        }
    }

    public void reconnect(String cause) {
        this.count++;
        this.delay = Math.min(2000 * this.count, 120000);
        this.cause = cause;

        // Update browser view scope with error info
        var bvScope = app.viewMap.get(ViewStateCoordinator.BROWSER_VSID);
        if (bvScope != null) {
            var state = bvScope.getState();
            state.put("error.cause", cause);
            state.put("error.numAttempt", count);
            state.put("error.delay", delay);
            bvScope.forceUpdate();
        }

        if (reconnectHandler == 0) {
            reconnectHandler = Timers.setInterval((Timers.TimerCallback) this::check, 1000);
        }
    }

    private void check() {
        var bvScope = app.viewMap.get(ViewStateCoordinator.BROWSER_VSID);
        if (bvScope == null) {
            reset();
            return;
        }

        if (app.isConnected) {
            reset();
            return;
        }

        var state = bvScope.getState();
        state.put("error.cause", cause);
        state.put("error.numAttempt", count);
        state.put("error.delay", delay);

        if (delay > 0) {
            delay -= 1000;
            if (delay < 0) delay = 0;
        }

        bvScope.forceUpdate();

        if (delay <= 0) {
            Timers.setTimeout((Timers.TimerCallback) app::assureContextExchangerIsConnected, 16);
        }
    }

    public void reset() {
        var browserView = app.viewMap.get(ViewStateCoordinator.BROWSER_VSID);
        if (browserView != null) {
            var state = browserView.getState();
            state.remove("error.cause");
            state.remove("error.numAttempt");
            state.remove("error.delay");
            browserView.forceUpdate();
        }

        count = 0;
        delay = 0;
        cause = "";

        if (reconnectHandler != 0) {
            Timers.clearInterval(reconnectHandler);
            reconnectHandler = 0;
        }
    }

    public void checkNow() {
        delay = 0;
        check();
    }
}
