package br.com.wdc.shopping.view.remote.shell.java.shopping.presentation;

import java.util.List;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of {@code RemoteBrowserPresenter} (classId {@value #CLASS_ID}).
 * <p>
 * The browser presenter is the top of the server-side view composition tree.
 * Its instanceId is always {@code "7b32e816a191:0"} ({@link HostClient#BROWSER_VSID}).
 *
 * <pre>
 * browser (7b32e816a191:0)      ← this presenter
 *   └── contentViewId → RootPresenter (f2d345c4a610:N)
 *         └── contentViewId → active page
 * </pre>
 *
 * State fields: {@code instanceId}, {@code alertId}, {@code alertArgs}, {@code contentViewId}.
 *
 * <pre>
 * Events:
 *   1  → onAlertOk()     no params
 *   2  → onKeepAlive()   no params
 *  -1  → navigate(path)  handled via HostClient.navigate() — not exposed here
 * </pre>
 */
public class BrowserPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "7b32e816a191";

    /**
     * Returns the browser presenter. The instanceId is always {@link HostClient#BROWSER_VSID}.
     */
    public static BrowserPresenterClient get(HostClient client) {
        return client.presenterByInstanceId(HostClient.BROWSER_VSID, BrowserPresenterClient.class);
    }

    public BrowserPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Confirms the current alert dialog. */
    public HostResponse onAlertOk() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    /** Sends a keep-alive ping to extend the server session lifetime. */
    public HostResponse onKeepAlive() throws InterruptedException, TimeoutException {
        return submit(2);
    }

    // :: State accessors

    /**
     * Returns the instanceId of the currently active root/frame presenter,
     * or {@code null} if not yet populated.
     */
    public String contentViewId() {
        var s = state();
        return s != null ? s.getString("contentViewId") : null;
    }

    /**
     * Returns the current alert code ({@code 0} when no alert is active).
     */
    public int alertId() {
        var s = state();
        if (s == null) return 0;
        var v = s.getLong("alertId");
        return v != null ? v.intValue() : 0;
    }

    /**
     * Returns the current alert arguments as strings, or an empty list when no alert is active.
     */
    @SuppressWarnings("unchecked")
    public List<String> alertArgs() {
        var s = state();
        if (s == null) return List.of();
        Object v = s.fields().get("alertArgs");
        if (v instanceof List<?> l) return (List<String>) l;
        return List.of();
    }

    /**
     * Returns {@code true} if an alert dialog is currently active ({@code alertId != 0}).
     */
    public boolean hasAlert() {
        return alertId() != 0;
    }

    // :: Composition helpers

    /**
     * Returns the root/frame presenter by following {@code contentViewId} from this presenter's state.
     *
     * @throws IllegalStateException if no root presenter is currently active
     */
    public RootPresenterClient root() {
        var id = contentViewId();
        if (id == null) throw new IllegalStateException(
                "No active root presenter in BrowserPresenter — contentViewId is null");
        return client.presenterByInstanceId(id, RootPresenterClient.class);
    }
}
