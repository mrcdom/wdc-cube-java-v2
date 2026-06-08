package br.com.wdc.shopping.view.remote.shell.java.shopping.presentation;

import java.util.Optional;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;

/**
 * Client mirror of {@code RootPresenter} (classId {@value #CLASS_ID}).
 * <p>
 * The root presenter is the frame/shell that sits between the browser ViewState
 * ({@code 7b32e816a191:0}) and the active page (login, home, etc.).
 *
 * <pre>
 * browser (7b32e816a191:0)
 *   └── contentViewId → RootPresenter (f2d345c4a610:N)
 *         └── contentViewId → active page (LoginPresenter | HomePresenter | ...)
 * </pre>
 *
 * Use {@link #get(HostClient)} to obtain an instance resolved from the browser
 * composition chain. Use {@link #pageAs(Class)} to get the currently active page
 * as a typed presenter.
 */
public class RootPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "f2d345c4a610";

    public RootPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    /**
     * Returns the {@code RootPresenterClient} by following the browser composition chain.
     * Equivalent to {@code BrowserPresenterClient.get(client).root()}.
     *
     * @return the root presenter, or {@link Optional#empty()} if the browser state is not yet available
     */
    public static Optional<RootPresenterClient> get(HostClient client) {
        var browserState = client.viewState(HostClient.BROWSER_VSID);
        if (browserState == null) return Optional.empty();
        var rootId = browserState.getString("contentViewId");
        if (rootId == null) return Optional.empty();
        return Optional.of(client.presenterByInstanceId(rootId, RootPresenterClient.class));
    }

    // :: State accessors

    /**
     * Returns the instanceId of the currently active page presenter
     * (e.g., {@code "c677cda52d14:2"} for login, {@code "473dbdd7a36a:3"} for home),
     * or {@code null} when no page is active.
     */
    public String contentViewId() {
        var s = state();
        return s != null ? s.getString("contentViewId") : null;
    }

    /**
     * Returns the currently active page as a typed presenter, using the
     * {@code contentViewId} field from this presenter's ViewState.
     *
     * @throws IllegalStateException if no page is currently active
     */
    public <T extends AbstractPresenterClient> T pageAs(Class<T> type) {
        var id = contentViewId();
        if (id == null) throw new IllegalStateException(
                "No active page in RootPresenter — contentViewId is null");
        return client.presenterByInstanceId(id, type);
    }
}
