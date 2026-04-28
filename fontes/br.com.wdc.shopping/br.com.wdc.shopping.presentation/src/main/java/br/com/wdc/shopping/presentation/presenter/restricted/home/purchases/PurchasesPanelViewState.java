package br.com.wdc.shopping.presentation.presenter.restricted.home.purchases;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;

public class PurchasesPanelViewState implements ViewState {

    public List<PurchaseInfo> purchases;
    public int page;
    public int pageSize = PurchasesPanelPresenter.DEFAULT_PAGE_SIZE;
    public int totalCount;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);
            json.name("page").value(this.page);
            json.name("pageSize").value(this.pageSize);
            json.name("totalCount").value(this.totalCount);

            json.name("purchases").beginArray();
            for (var purchase : Optional.ofNullable(this.purchases).orElse(Collections.emptyList())) {
                json.beginObject();
                {
                    json.name("id").value(purchase.id);
                    json.name("date").value(purchase.date);
                    json.name("total").value(purchase.total);
                    json.name("items").beginArray();
                    if (purchase.items != null) {
                        purchase.items.forEach(json::value);
                    }
                    json.endArray();
                }
                json.endObject();
            }
            json.endArray();
        }
        json.endObject();
    }

}
