package br.com.wdc.shopping.presentation.presenter.open.login;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.ViewState;

public class LoginViewState implements ViewState {

    public String userName;
    public String password;
    public int errorCode;
    public String errorMessage;

    @Override
    public void write(String instanceId, ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(instanceId);

            if (StringUtils.isNotBlank(this.userName)) {
                json.name("userName").value(this.userName);
            }

            if (StringUtils.isNotBlank(this.errorMessage)) {
                json.name("errorMessage").value(this.errorMessage);
            }
        }
        json.endObject();
    }

}
