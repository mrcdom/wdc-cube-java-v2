package br.com.wdc.shopping.presentation.presenter.restricted.receipt;

import java.util.Collections;
import java.util.Optional;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;

public class ReceiptViewState implements ViewState {

    public boolean notifySuccess;
    public ReceiptForm receipt;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            json.name("notifySuccess").value(this.notifySuccess);
            this.notifySuccess = false;

            if (this.receipt != null) {
                json.name("receipt").beginObject();
                {
                    json.name("date").value(receipt.date);
                    json.name("total").value(receipt.total);

                    json.name("items").beginArray();
                    for (var receiptItem : Optional.ofNullable(receipt.items).orElse(Collections.emptyList())) {
                        json.beginObject();
                        {
                            json.name("description").value(receiptItem.description);
                            json.name("value").value(receiptItem.value);
                            json.name("quantity").value(receiptItem.quantity);
                        }
                        json.endObject();
                    }
                    json.endArray();
                }
                json.endObject();
            }
        }
        json.endObject();
    }

}
