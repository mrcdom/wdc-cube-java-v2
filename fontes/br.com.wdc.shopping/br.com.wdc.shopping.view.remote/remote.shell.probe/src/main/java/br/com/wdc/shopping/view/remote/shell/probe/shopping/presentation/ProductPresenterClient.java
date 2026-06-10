package br.com.wdc.shopping.view.remote.shell.probe.shopping.presentation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of {@code ProductPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onOpenProducts()            no params
 *   2 → onAddToCart(quantity)       param: p.quantity
 * </pre>
 */
public class ProductPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "48b693f67410";

    public static Optional<ProductPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, ProductPresenterClient.class);
    }

    public static List<ProductPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, ProductPresenterClient.class);
    }

    public ProductPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    public ProductPresenterClient(String instanceId, HostClient client) {
        super(instanceId, client);
    }

    // :: Events

    /** Triggers {@code ProductPresenter.onOpenProducts()}. */
    public HostResponse onOpenProducts() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    /** Triggers {@code ProductPresenter.onAddToCart(quantity)}. */
    public HostResponse onAddToCart(int quantity) throws InterruptedException, TimeoutException {
        param("quantity", quantity);
        return submit(2);
    }

    // :: State accessors

    /** The current product map ({@code id}, {@code name}, {@code description}, etc.). */
    public Map<String, Object> product() {
        var s = state();
        if (s == null)
            return null;
        var v = s.fields().get("product");
        if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            var typed = (Map<String, Object>) m;
            return typed;
        }
        return null;
    }

    public Long productId() {
        var p = product();
        if (p == null)
            return null;
        var v = p.get("id");
        return v instanceof Number n ? n.longValue() : null;
    }

    public String productName() {
        var p = product();
        return p != null ? String.valueOf(p.get("name")) : null;
    }
}
