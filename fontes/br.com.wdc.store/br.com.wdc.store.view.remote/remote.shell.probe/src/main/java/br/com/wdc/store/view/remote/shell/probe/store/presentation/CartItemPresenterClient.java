package br.com.wdc.store.view.remote.shell.probe.store.presentation;

import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of a cart row ({@code CartScope.CartItemScope}, classId {@value #CLASS_ID}): a view of its own, whose
 * actions are the row's — no product id goes back.
 *
 * <pre>
 * Events:
 *   1 → onRemove()                 no params
 *   2 → onModifyQuantity(qty)      param: p.quantity
 * </pre>
 */
public class CartItemPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "c8a1f0e2b7d4";

    public CartItemPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    public HostResponse onRemove() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    public HostResponse onModifyQuantity(int quantity) throws InterruptedException, TimeoutException {
        param("quantity", quantity);
        return submit(2);
    }

    // :: Scope accessors

    public long id() {
        var s = scope();
        var v = s != null ? s.getLong("id") : null;
        return v != null ? v : -1L;
    }

    public long quantity() {
        var s = scope();
        var v = s != null ? s.getLong("quantity") : null;
        return v != null ? v : 0L;
    }
}
