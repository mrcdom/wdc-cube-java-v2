package br.com.wdc.framework.cube.remote.bridge.teavm;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Timers;

/**
 * Controls reconnection with exponential backoff.
 */
public class ReconnectController {

    private final ScopeCoordinator app;

    String url = "";
    int count = 0;
    int reconnectHandler = 0;
    int delay = 0;
    String cause = "";

    public ReconnectController(ScopeCoordinator app) {
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

        var bvScope = app.viewMap.get(ScopeCoordinator.BROWSER_VSID);
        if (bvScope != null) {
            var values = bvScope.getValues();
            values.put("error.cause", cause);
            values.put("error.numAttempt", count);
            values.put("error.delay", delay);
            bvScope.forceUpdate();
        }

        if (reconnectHandler == 0) {
            reconnectHandler = Timers.setInterval((Timers.TimerCallback) this::check, 1000);
        }
    }

    private void check() {
        var bvScope = app.viewMap.get(ScopeCoordinator.BROWSER_VSID);
        if (bvScope == null) {
            reset();
            return;
        }

        if (app.isConnected) {
            reset();
            return;
        }

        var values = bvScope.getValues();
        values.put("error.cause", cause);
        values.put("error.numAttempt", count);
        values.put("error.delay", delay);

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
        var browserView = app.viewMap.get(ScopeCoordinator.BROWSER_VSID);
        if (browserView != null) {
            var values = browserView.getValues();
            values.remove("error.cause");
            values.remove("error.numAttempt");
            values.remove("error.delay");
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
