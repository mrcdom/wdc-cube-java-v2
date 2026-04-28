package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class RootReactViewImpl extends GenericViewImpl {

    protected RootPresenter presenter;

    public RootReactViewImpl(RootPresenter presenter) {
        super(presenter.app, "f2d345c4a610");
        this.presenter = presenter;
        this.app.setRootPresenter(presenter);
    }

    @Override
    public void release() {
        this.app.setRootPresenter(null);
        super.release();
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        // NOOP
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        var state = this.presenter.state;

        json.beginObject();
        {
            json.name("id").value(this.instanceId);

            if (state.contentView instanceof GenericViewImpl view) {
                json.name("contentViewId").value(view.instanceId());
            }

            if (StringUtils.isNotBlank(state.errorMessage)) {
                json.name("errorMessage").value(state.errorMessage);
            }
        }
        json.endObject();
    }

}
