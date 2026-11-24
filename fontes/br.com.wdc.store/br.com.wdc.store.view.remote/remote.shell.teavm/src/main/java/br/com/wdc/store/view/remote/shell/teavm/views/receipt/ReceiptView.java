package br.com.wdc.store.view.remote.shell.teavm.views.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.receipt.ReceiptSharedView;

public class ReceiptView extends AbstractRemoteView {

    public static final String VIEW_ID = ReceiptScope.CLASS_ID;

    private static final int ON_BACK = 1;

    private final ReceiptSharedView shared;
    private final ReceiptScope adaptedScope = new ReceiptScope();

    public ReceiptView(String vsid) {
        super(vsid);

        this.shared = new ReceiptSharedView();
        this.shared.scopeSupplier = this::adaptScope;
        this.shared.onBack = () -> submit(ON_BACK);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private ReceiptScope adaptScope() {
        ViewScope scope = scope();
        adaptedScope.notifySuccess.set(scope.getBoolean("notifySuccess"));
        adaptedScope.receipt.set(adaptReceipt(scope));
        return adaptedScope;
    }

    @SuppressWarnings("unchecked")
    private ReceiptForm adaptReceipt(ViewScope scope) {
        Map<String, Object> map = scope.getMap("receipt");
        if (map.isEmpty()) return null;

        var form = new ReceiptForm();
        var dateVal = CoerceUtils.asNumber(map.get("date"));
        form.date = dateVal != null ? dateVal.longValue() : null;
        var totalVal = CoerceUtils.asNumber(map.get("total"));
        form.total = totalVal != null ? totalVal.doubleValue() : null;

        var itemsObj = map.get("items");
        if (itemsObj instanceof List<?> list) {
            var items = new ArrayList<ReceiptItem>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) {
                    items.add(mapToReceiptItem((Map<String, Object>) m));
                }
            }
            form.items = items;
        } else {
            form.items = List.of();
        }
        return form;
    }

    private ReceiptItem mapToReceiptItem(Map<String, Object> m) {
        var item = new ReceiptItem();
        item.description = CoerceUtils.asString(m.get("description"), "");
        item.quantity = CoerceUtils.asInteger(m.get("quantity"), 1);
        var valObj = CoerceUtils.asNumber(m.get("value"));
        item.value = valObj != null ? valObj.doubleValue() : 0;
        return item;
    }
}
