package br.com.wdc.shopping.presentation.presenter.restricted.home.products;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class ProductsPanelViewState implements ViewState {

    public List<ProductInfo> products;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            json.name("products").beginArray();
            for (var product : Optional.ofNullable(products).orElse(Collections.emptyList())) {
                json.beginObject();
                {
                    json.name("id").value(product.id);
                    json.name("image").value(product.image);
                    json.name("name").value(product.name);
                    json.name("description").value(product.description);
                    json.name("price").value(product.price);
                }
                json.endObject();
            }
            json.endArray();
        }
        json.endObject();
    }

}
