package br.com.wdc.shopping.view.remote.skeleton.util;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.view.remote.skeleton.viewimpl.ApplicationReactImpl;

public final class GenericViewImpl implements CubeView {

    protected final ApplicationReactImpl app;
    protected final PresenterBase presenter;
    protected final ViewState state;
    protected final CubeSkeleton skeleton;
    protected final String instanceId;

    protected int alertId;

    public GenericViewImpl(ShoppingApplication app, PresenterBase presenter, ViewState state, CubeSkeleton skeleton) {
        this(app, presenter, state, skeleton, skeleton.classId() + ":" + ((ApplicationReactImpl) app).nextInstanceId());
    }

    public GenericViewImpl(ShoppingApplication app, PresenterBase presenter, ViewState state, CubeSkeleton skeleton,
            String vsid) {
        this.app = (ApplicationReactImpl) app;
        this.presenter = presenter;
        this.state = state;
        this.skeleton = skeleton;
        this.instanceId = vsid;
        this.app.putView(this);
        this.app.markDirty(this);
    }

    @Override
    public String instanceId() {
        return this.instanceId;
    }

    public PresenterBase presenter() {
        return this.presenter;
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
        this.skeleton.syncState(formData);
    }

    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        this.skeleton.submit(eventCode, eventQtde, formData);
    }

    public void writeState(ExtensibleObjectOutput json) {
        new ViewStateSerializer(json).write(this.state, instanceId);
        // state.write(instanceId, json);
    }

}
