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
 * Client mirror of {@code ReceiptPresenter} (classId {@value #CLASS_ID}).
 *
 * <pre>
 * Events:
 *   1 → onOpenProducts()   no params
 * </pre>
 */
public class ReceiptPresenterClient extends AbstractPresenterClient {

    public static final String CLASS_ID = "e8d0bd8ae3bc";

    public static Optional<ReceiptPresenterClient> getFirst(HostClient client) {
        return client.firstPresenterByClass(CLASS_ID, ReceiptPresenterClient.class);
    }

    public static List<ReceiptPresenterClient> getAll(HostClient client) {
        return client.presentersByClass(CLASS_ID, ReceiptPresenterClient.class);
    }

    public ReceiptPresenterClient(HostClient client, String vsid) {
        super(vsid, client);
    }

    // :: Events

    /** Triggers {@code ReceiptPresenter.onOpenProducts()}. */
    public HostResponse onOpenProducts() throws InterruptedException, TimeoutException {
        return submit(1);
    }

    // :: State accessors

    public boolean notifySuccess() {
        var s = state();
        return s != null && Boolean.TRUE.equals(s.getBoolean("notifySuccess"));
    }

    /** The receipt map ({@code id}, {@code items}, {@code total}, etc.). */
    public Map<String, Object> receipt() {
        var s = state();
        if (s == null)
            return Collections.emptyMap();
        var v = s.fields().get("receipt");
        if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            var typed = (Map<String, Object>) m;
            return typed;
        }
        return Collections.emptyMap();
    }
}
