package br.com.wdc.shopping.view.react.skeleton.util;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.view.react.skeleton.viewimpl.ApplicationReactImpl;

public abstract class GenericViewImpl implements CubeView {

    protected final ApplicationReactImpl app;
    protected final String instanceId;

    protected int alertId;

    protected GenericViewImpl(ShoppingApplication app, String vid) {
        this(app, vid, ((ApplicationReactImpl) app).nextInstanceId());
    }

    protected GenericViewImpl(ShoppingApplication app, String vid, int instanceId) {
        this.app = (ApplicationReactImpl) app;
        this.instanceId = vid + ":" + instanceId;
        this.app.putView(this);
        this.app.markDirty(this);
    }

    public final String instanceId() {
        return this.instanceId;
    }

    @Override
    public void release() {
        this.app.removeView(this.instanceId);
    }

    @Override
    public void update() {
        this.app.markDirty(this);
    }

    public void syncClientToServer(Map<String, Object> formData) {
        // NOOP
    }

    public abstract void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception;

    public abstract void writeState(ExtensibleObjectOutput json);

}
