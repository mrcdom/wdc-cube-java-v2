package br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of {@code CartPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onBuy()                              no params
 *   3 → onOpenProducts()                     no params
 * </pre>
 *
 * The rows are views of their own ({@link CartItemPresenterClient}), referred to by {@code rowsViewIds}.
 */
public class CartPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "7eb485e5f843";
    
    public static Optional<CartPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, CartPresenterClient.class);
    }

    public static List<CartPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, CartPresenterClient.class);
    }

    public CartPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Triggers {@code CartPresenter.onBuy()}. */
    public HostResponse onBuy() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    /** Triggers {@code CartPresenter.onOpenProducts()}. */
    public HostResponse onOpenProducts() throws InterruptedException, TimeoutException {
        return submit(3);
    }

    // :: State accessors

    /** The rows, in the cart's order: each one a view of its own. */
    public List<CartItemPresenterClient> rows() {
        var s = state();
        if (s == null || !(s.fields().get("rowsViewIds") instanceof List<?> ids)) {
            return Collections.emptyList();
        }
        var rows = new ArrayList<CartItemPresenterClient>(ids.size());
        for (var id : ids) {
            rows.add(client.presenterByInstanceId(String.valueOf(id), CartItemPresenterClient.class));
        }
        return rows;
    }

    public int itemCount() {
        return rows().size();
    }

    public double total() {
        var s = state();
        var v = s != null ? s.getDouble("total") : null;
        return v != null ? v : 0.0;
    }
}
