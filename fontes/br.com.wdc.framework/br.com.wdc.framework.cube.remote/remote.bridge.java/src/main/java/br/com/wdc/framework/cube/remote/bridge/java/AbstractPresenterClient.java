package br.com.wdc.framework.cube.remote.bridge.java;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;
import br.com.wdc.framework.cube.remote.bridge.java.model.ViewStateSnapshot;

/**
 * Base class for typed client-side presenter mirrors.
 * <p>
 * A presenter client:
 * <ul>
 *   <li>Knows its {@code instanceId} (e.g., {@code "c677cda52d14:0"})</li>
 *   <li>Exposes {@code onXxx()} methods that map 1-to-1 to the server presenter's events</li>
 *   <li>Accumulates form field values via {@link #param} / {@link #field} before each submit</li>
 *   <li>Reads current view state via {@link #state()} — always fresh from the live {@link HostClient}</li>
 * </ul>
 *
 * <h3>Parameter prefix convention</h3>
 * When an {@code onXxx} method has parameters, the server expects the form fields prefixed
 * with {@code "p."} (e.g., {@code "p.productId"}, {@code "p.quantity"}).
 * Use {@link #param(String, Object)} for those fields.
 * Use {@link #field(String, Object)} for direct fields (no prefix).
 */
public abstract class AbstractPresenterClient {

    protected final String instanceId;
    protected final HostClient client;

    private final Map<String, Object> form = new LinkedHashMap<>();

    protected AbstractPresenterClient(String instanceId, HostClient client) {
        this.instanceId = instanceId;
        this.client = client;
    }

    // :: Form building

    /**
     * Adds a method-parameter field (automatically prefixed with {@code "p."}) to the pending form.
     */
    protected void param(String name, Object value) {
        form.put("p." + name, value);
    }

    /**
     * Adds a direct field (no prefix) to the pending form.
     */
    protected void field(String name, Object value) {
        form.put(name, value);
    }

    // :: Submit

    /**
     * Sends the accumulated form to the server for the given event code and waits for the
     * response (default 10-second timeout). The form is cleared after sending regardless of outcome.
     */
    protected HostResponse submit(int eventCode) throws InterruptedException, TimeoutException {
        return submit(eventCode, null);
    }

    /**
     * Sends the accumulated form to the server for the given event code and waits for the
     * response up to {@code timeout}. The form is cleared after sending regardless of outcome.
     */
    protected HostResponse submit(int eventCode, Duration timeout) throws InterruptedException, TimeoutException {
        var snapshot = Map.copyOf(form);
        form.clear();
        long reqId = client.submit(instanceId, eventCode, snapshot);
        return timeout != null ? client.awaitResponseFor(reqId, timeout) : client.awaitResponseFor(reqId);
    }

    // :: State access

    /**
     * Returns the current {@link ViewStateSnapshot} for this presenter, or {@code null}
     * if the server has not yet pushed a state for this instance.
     */
    public ViewStateSnapshot state() {
        return client.viewState(instanceId);
    }

    /** The presenter's instance ID (e.g., {@code "c677cda52d14:0"}). */
    public String instanceId() {
        return instanceId;
    }

    // :: Common state accessors (available on most presenters)

    /** Returns the current {@code errorCode} from the view state, or {@code 0} if absent. */
    public int errorCode() {
        var s = state();
        if (s == null) return 0;
        var v = s.getLong("errorCode");
        return v != null ? v.intValue() : 0;
    }

    /** Returns the current {@code errorMessage} from the view state, or {@code null} if absent. */
    public String errorMessage() {
        var s = state();
        return s != null ? s.getString("errorMessage") : null;
    }

    /** Returns {@code true} if the current {@code errorCode} is non-zero. */
    public boolean hasError() {
        return errorCode() != 0;
    }
}
