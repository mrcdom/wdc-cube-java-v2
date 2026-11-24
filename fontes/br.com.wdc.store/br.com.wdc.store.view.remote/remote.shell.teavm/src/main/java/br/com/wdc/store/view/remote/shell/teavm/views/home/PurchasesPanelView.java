package br.com.wdc.store.view.remote.shell.teavm.views.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.home.PurchasesPanelSharedView;

public class PurchasesPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = HomeScope.PurchasesPanelScope.CLASS_ID;

    private static final int ON_OPEN_RECEIPT = 1;
    private static final int ON_PAGE_CHANGE = 2;
    private static final int ON_PAGE_SIZE_CHANGED = 3;

    private final PurchasesPanelSharedView shared;
    private final HomeScope.PurchasesPanelScope adaptedScope = new HomeScope.PurchasesPanelScope();

    public PurchasesPanelView(String vsid) {
        super(vsid);

        this.shared = new PurchasesPanelSharedView();
        this.shared.scopeSupplier = this::adaptScope;
        this.shared.requestUpdate = this::forceUpdate;
        this.shared.onOpenReceipt = id -> {
            setFormField("p.purchaseId", id);
            submit(ON_OPEN_RECEIPT);
        };
        this.shared.onPageChange = page -> {
            setFormField("p.page", page);
            submit(ON_PAGE_CHANGE);
        };
        this.shared.onPageSizeChanged = capacity -> {
            setFormField("p.capacity", capacity);
            submit(ON_PAGE_SIZE_CHANGED);
        };
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        shared.afterUpdate();
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private HomeScope.PurchasesPanelScope adaptScope() {
        ViewScope scope = scope();
        adaptedScope.purchases.set(getPurchases(scope));
        adaptedScope.page.set(scope.getInt("page"));
        adaptedScope.pageSize.set(scope.getInt("pageSize"));
        adaptedScope.totalCount.set(scope.getInt("totalCount"));
        return adaptedScope;
    }

    @SuppressWarnings("unchecked")
    private List<PurchaseInfo> getPurchases(ViewScope scope) {
        if (scope == null) return List.of();
        var v = scope.getValues().get("purchases");
        if (v instanceof List<?> list) {
            var result = new ArrayList<PurchaseInfo>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m && m.containsKey("id")) {
                    result.add(mapToPurchaseInfo((Map<String, Object>) m));
                }
            }
            return result;
        }
        return List.of();
    }

    private PurchaseInfo mapToPurchaseInfo(Map<String, Object> m) {
        var info = new PurchaseInfo();
        info.id = CoerceUtils.asLong(m.get("id"), 0L);
        var dateVal = CoerceUtils.asNumber(m.get("date"));
        info.date = dateVal != null ? dateVal.longValue() : 0L;
        var totalVal = CoerceUtils.asNumber(m.get("total"));
        info.total = totalVal != null ? totalVal.doubleValue() : 0;
        var itemsObj = m.get("items");
        if (itemsObj instanceof List<?> il) {
            info.items = il.stream().map(Object::toString).toList();
        } else {
            info.items = List.of();
        }
        return info;
    }
}
