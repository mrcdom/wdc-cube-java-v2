package br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.javaclient.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.javaclient.HostClient;
import br.com.wdc.framework.cube.remote.javaclient.model.HostResponse;

/**
 * Client mirror of {@code CartPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onBuy()                              no params
 *   2 → onRemoveProduct(productId)           param: p.productId
 *   3 → onOpenProducts()                     no params
 *   4 → onModifyQuantity(productId, qty)     params: p.productId, p.quantity
 * </pre>
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

    /** Triggers {@code CartPresenter.onRemoveProduct(productId)}. */
    public HostResponse onRemoveProduct(long productId) throws InterruptedException, TimeoutException {
        param("productId", productId);
        return submit(2);
    }

    /** Triggers {@code CartPresenter.onOpenProducts()}. */
    public HostResponse onOpenProducts() throws InterruptedException, TimeoutException {
        return submit(3);
    }

    /** Triggers {@code CartPresenter.onModifyQuantity(productId, quantity)}. */
    public HostResponse onModifyQuantity(long productId, int quantity)
            throws InterruptedException, TimeoutException {
        param("productId", productId);
        param("quantity", quantity);
        return submit(4);
    }

    // :: State accessors

    /** The list of cart items ({@code productId}, {@code productName}, {@code quantity}, etc.). */
    public List<Map<String, Object>> items() {
        var s = state();
        if (s == null) return Collections.emptyList();
        return s.getList("items");
    }

    public int itemCount() {
        return items().size();
    }
}
