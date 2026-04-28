package br.com.wdc.shopping.presentation.presenter;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ViewState;

public class RootViewState implements ViewState {

    public CubeView contentView;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            if (this.contentView != null) {
                json.name("contentViewId").value(this.contentView.instanceId());
            }

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }

}
