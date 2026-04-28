package br.com.wdc.shopping.presentation.presenter.restricted.products;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class ProductViewState implements ViewState {

    public ProductInfo product;
    public int errorCode;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            if (this.product != null) {
                json.name("product").beginObject();
                {
                    json.name("id").value(this.product.id);
                    json.name("name").value(this.product.name);
                    json.name("description").value(this.product.description);
                    json.name("price").value(this.product.price);
                }
                json.endObject();
            }

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }

}
