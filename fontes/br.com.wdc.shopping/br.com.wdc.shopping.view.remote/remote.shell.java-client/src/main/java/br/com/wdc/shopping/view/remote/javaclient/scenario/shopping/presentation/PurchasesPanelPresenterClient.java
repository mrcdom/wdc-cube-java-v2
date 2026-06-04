package br.com.wdc.shopping.view.remote.javaclient.scenario.shopping.presentation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import br.com.wdc.shopping.view.remote.javaclient.AbstractPresenterClient;
import br.com.wdc.shopping.view.remote.javaclient.HostClient;
import br.com.wdc.shopping.view.remote.javaclient.model.HostResponse;

/**
 * Client mirror of {@code PurchasesPanelPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onOpenReceipt(purchaseId)          param: p.purchaseId
 *   2 → onPageChange(page)                 param: p.page
 *   3 → onItemSizeCapacityChanged(cap)     param: p.capacity
 * </pre>
 */
public class PurchasesPanelPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "b3c4d5e6f7a8";

    public static Optional<PurchasesPanelPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, PurchasesPanelPresenterClient.class);
    }

    public static List<PurchasesPanelPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, PurchasesPanelPresenterClient.class);
    }

    public PurchasesPanelPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Triggers {@code PurchasesPanelPresenter.onOpenReceipt(purchaseId)}. */
    public HostResponse onOpenReceipt(long purchaseId) throws InterruptedException, TimeoutException {
        param("purchaseId", purchaseId);
        return submit(1);
    }

    /** Triggers {@code PurchasesPanelPresenter.onPageChange(page)}. */
    public HostResponse onPageChange(int page) throws InterruptedException, TimeoutException {
        param("page", page);
        return submit(2);
    }

    /**
     * Triggers {@code PurchasesPanelPresenter.onItemSizeCapacityChanged(capacity)}. Use this to set how many items the
     * "view" can display — controls the server-side page size.
     */
    public HostResponse onItemSizeCapacityChanged(int capacity)
            throws InterruptedException, TimeoutException {
        param("capacity", capacity);
        return submit(3);
    }

    // :: State accessors

    public List<Map<String, Object>> purchases() {
        var s = state();
        if (s == null)
            return Collections.emptyList();
        return s.getList("purchases");
    }

    public int page() {
        var s = state();
        if (s == null)
            return 0;
        var v = s.getLong("page");
        return v != null ? v.intValue() : 0;
    }

    public int pageSize() {
        var s = state();
        if (s == null)
            return -1;
        var v = s.getLong("pageSize");
        return v != null ? v.intValue() : -1;
    }

    public int totalCount() {
        var s = state();
        if (s == null)
            return 0;
        var v = s.getLong("totalCount");
        return v != null ? v.intValue() : 0;
    }
}
