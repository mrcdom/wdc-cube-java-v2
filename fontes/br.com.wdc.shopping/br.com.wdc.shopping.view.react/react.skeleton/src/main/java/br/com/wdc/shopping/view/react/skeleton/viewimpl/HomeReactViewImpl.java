package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class HomeReactViewImpl extends GenericViewImpl {

    protected HomePresenter presenter;

    public HomeReactViewImpl(HomePresenter presenter) {
        super(presenter.app, "473dbdd7a36a");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> presenter.onExit();
        case 2 -> presenter.onOpenCart();
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
