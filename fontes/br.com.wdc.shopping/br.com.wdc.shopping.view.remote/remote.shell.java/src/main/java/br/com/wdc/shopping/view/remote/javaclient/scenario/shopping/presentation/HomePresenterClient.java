package br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.javaclient.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.javaclient.HostClient;
import br.com.wdc.framework.cube.remote.javaclient.model.HostResponse;

/**
 * Client mirror of {@code HomePresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onExit()        no params
 *   2 → onOpenCart()    no params
 * </pre>
 *
 * Note: {@code onOpenProduct} and {@code onOpenReceipt} are NOT in this skeleton —
 * they are triggered via {@link ProductsPanelPresenterClient#onOpenProduct(long)}
 * and {@link PurchasesPanelPresenterClient#onOpenReceipt(long)} respectively,
 * which delegate internally to the Home presenter on the server.
 */
public class HomePresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "473dbdd7a36a";

    public static Optional<HomePresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, HomePresenterClient.class);
    }

    public static List<HomePresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, HomePresenterClient.class);
    }

    public HomePresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Triggers {@code HomePresenter.onExit()}. */
    public HostResponse onExit() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    /** Triggers {@code HomePresenter.onOpenCart()}. */
    public HostResponse onOpenCart() throws InterruptedException, TimeoutException {
        return submit(2);
    }

    // :: State accessors

    public String nickName() {
        var s = state();
        return s != null ? s.getString("nickName") : null;
    }

    public int cartItemCount() {
        var s = state();
        if (s == null) return 0;
        var v = s.getLong("cartItemCount");
        return v != null ? v.intValue() : 0;
    }

    /**
     * Returns the instanceId of the currently active sub-view (e.g., a product or cart view),
     * or {@code null} when the home screen has no active sub-view open.
     */
    public String contentViewId() {
        var s = state();
        return s != null ? s.getString("contentViewId") : null;
    }

    /**
     * Returns the products panel sub-view as a typed presenter, using the
     * {@code productsPanelViewId} from this presenter's ViewState.
     */
    public ProductsPanelPresenterClient productPanel() {
        var s = state();
        if (s == null) throw new IllegalStateException("HomePresenter state not available");
        var id = s.getString("productsPanelViewId");
        if (id == null) throw new IllegalStateException("productsPanelViewId not present in HomePresenter state");
        return client.presenterByInstanceId(id, ProductsPanelPresenterClient.class);
    }

    /**
     * Returns the purchases panel sub-view as a typed presenter, using the
     * {@code purchasesPanelViewId} from this presenter's ViewState.
     */
    public PurchasesPanelPresenterClient purchasesPanel() {
        var s = state();
        if (s == null) throw new IllegalStateException("HomePresenter state not available");
        var id = s.getString("purchasesPanelViewId");
        if (id == null) throw new IllegalStateException("purchasesPanelViewId not present in HomePresenter state");
        return client.presenterByInstanceId(id, PurchasesPanelPresenterClient.class);
    }

    /**
     * Returns the currently active content sub-view as the given typed presenter,
     * using the {@code contentViewId} from this presenter's ViewState.
     *
     * @throws IllegalStateException if no content view is currently active
     */
    public <T extends AbstractPresenterClient> T contentViewAs(Class<T> type) {
        var id = contentViewId();
        if (id == null) throw new IllegalStateException(
                "No active contentView in HomePresenter — the home screen has no sub-view open");
        return client.presenterByInstanceId(id, type);
    }
}
