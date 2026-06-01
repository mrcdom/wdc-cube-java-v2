package br.com.wdc.framework.cube.remote;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.ViewState;

/**
 * Generic {@link CubeView} implementation for remote applications.
 * <p>
 * Each presenter gets one instance that manages dirty-tracking and
 * delegates submit/syncState to the skeleton.
 */
public class RemoteViewImpl implements CubeView {

    protected final RemoteApplication app;
    protected final PresenterBase presenter;
    protected final ViewState state;
    protected final CubeSkeleton skeleton;
    protected final String instanceId;

    protected int alertId;

    public RemoteViewImpl(RemoteApplication app, PresenterBase presenter, ViewState state, CubeSkeleton skeleton) {
        this(app, presenter, state, skeleton, skeleton.classId() + ":" + app.nextInstanceId());
    }

    public RemoteViewImpl(RemoteApplication app, PresenterBase presenter, ViewState state, CubeSkeleton skeleton,
            String vsid) {
        this.app = app;
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
    }
}
