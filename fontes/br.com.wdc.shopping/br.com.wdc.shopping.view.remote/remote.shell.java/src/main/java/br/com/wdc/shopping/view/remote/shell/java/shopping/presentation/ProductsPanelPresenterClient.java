package br.com.wdc.shopping.view.remote.shell.java.shopping.presentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.framework.cube.remote.bridge.java.AbstractPresenterClient;
import br.com.wdc.framework.cube.remote.bridge.java.HostClient;
import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;

/**
 * Client mirror of {@code ProductsPanelPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onOpenProduct(productId)   param: p.productId
 * </pre>
 */
public class ProductsPanelPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "a1b2c3d4e5f6";

    public static Optional<ProductsPanelPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, ProductsPanelPresenterClient.class);
    }

    public static List<ProductsPanelPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, ProductsPanelPresenterClient.class);
    }

    public ProductsPanelPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Triggers {@code ProductsPanelPresenter.onOpenProduct(productId)}. */
    public HostResponse onOpenProduct(long productId) throws InterruptedException, TimeoutException {
        param("productId", productId);
        return submit(1);
    }

    // :: State accessors

    public List<Map<String, Object>> products() {
        var s = state();
        if (s == null)
            return Collections.emptyList();
        return s.getList("products");
    }
}
