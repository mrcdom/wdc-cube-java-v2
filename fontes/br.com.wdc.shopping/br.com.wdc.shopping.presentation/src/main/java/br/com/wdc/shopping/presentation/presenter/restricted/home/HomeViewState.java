package br.com.wdc.shopping.presentation.presenter.restricted.home;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ViewState;

public class HomeViewState implements ViewState {

    public CubeView contentView;
    public CubeView productsPanelView;
    public CubeView purchasesPanelView;
    public String nickName;
    public int cartItemCount;
    public int errorCode;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            if (StringUtils.isNotBlank(this.nickName)) {
                json.name("nickName").value(this.nickName);
            }

            json.name("cartItemCount").value(this.cartItemCount);

            if (this.contentView != null) {
                json.name("contentViewId").value(this.contentView.instanceId());
            }

            if (this.productsPanelView != null) {
                json.name("productsPanelViewId").value(this.productsPanelView.instanceId());
            }

            if (this.purchasesPanelView != null) {
                json.name("purchasesPanelViewId").value(this.purchasesPanelView.instanceId());
            }

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }

}
