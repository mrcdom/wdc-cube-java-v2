package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;

public class CartViewState implements ViewState {

    public List<CartItem> items;
    public int errorCode;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            json.name("items").beginArray();
            if (this.items != null) {
                this.items.forEach(item -> {
                    json.beginObject();
                    {
                        json.name("id").value(item.id);
                        json.name("name").value(item.name);
                        json.name("price").value(item.price);
                        json.name("quantity").value(item.quantity);
                    }
                    json.endObject();
                });
            }
            json.endArray();

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }
}
