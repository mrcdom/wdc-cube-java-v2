package br.com.wdc.framework.cube.remote;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;

/**
 * Generic {@link CubeView} implementation for remote applications.
 * <p>
 * Each scope drawn for a client (a presenter's, a segment's, a row's) gets one instance, made by
 * {@link #forScope}: it manages dirty-tracking and delegates submit/syncScope to the scope's skeleton, which
 * dispatches to the actions the presenter bound with an event code.
 */
public class RemoteViewImpl implements CubeView {

    protected final RemoteApplication app;
    protected final Scope scope;
    protected final CubeSkeleton skeleton;
    protected final String instanceId;

    /** The view of a scope: its skeleton dispatches to the actions the presenter bound on the scope. */
    public RemoteViewImpl(RemoteApplication app, Scope scope, String classId) {
        this(app, scope, classId, classId + ":" + app.nextInstanceId());
    }

    /** The same, with a given instance id (the browser's, which every client knows). */
    public RemoteViewImpl(RemoteApplication app, Scope scope, String classId, String instanceId) {
        this.app = app;
        this.scope = scope;
        this.skeleton = scope.skeleton(classId);
        this.instanceId = instanceId;
        this.app.putView(this);
        this.app.markDirty(this);
    }

    /**
     * The remote host's view for any scope: what a client knows it by is the {@code CLASS_ID} the scope class
     * declares (a {@code public static final String}). Meant as the fallback of the host's
     * {@link br.com.wdc.framework.cube.ScopeViews}.
     */
    public static RemoteViewImpl forScope(CubeApplication app, Scope scope) {
        return new RemoteViewImpl((RemoteApplication) app, scope, classIdOf(scope.getClass()));
    }

    private static String classIdOf(Class<?> scopeClass) {
        try {
            var field = scopeClass.getField("CLASS_ID");
            return (String) field.get(null);
        } catch (ReflectiveOperationException | ClassCastException caught) {
            throw new IllegalStateException(scopeClass.getName() + " declares no public static String CLASS_ID", caught);
        }
    }

    @Override
    public String instanceId() {
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
        this.skeleton.syncScope(formData);
    }

    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        this.skeleton.submit(eventCode, eventQtde, formData);
    }

    public void writeScope(ExtensibleObjectOutput json) {
        new ScopeSerializer(json, this::viewOf).write(this.scope, instanceId);
    }

    /** The view of a scope inside this one (a segment, a row): made the first time it is written. */
    private CubeView viewOf(Scope scope) {
        return this.app instanceof CubeApplication cubeApp ? cubeApp.viewOf(scope) : scope.view();
    }
}
