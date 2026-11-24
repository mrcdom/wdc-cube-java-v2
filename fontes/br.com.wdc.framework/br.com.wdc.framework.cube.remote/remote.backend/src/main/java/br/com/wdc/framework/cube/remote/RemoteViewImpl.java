package br.com.wdc.framework.cube.remote;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.Scope;

/**
 * What the remote host draws a scope with: the client's view of it, known by an instance id.
 * <p>
 * Each scope drawn for a client (a presenter's, a segment's, a row's) gets one, made by the application
 * ({@link RemoteApplicationSupport#viewOf}) the first time a scope that holds it is written: it tracks whether the
 * client needs the scope again, and delegates submit/syncScope to the scope's skeleton, which dispatches to the actions
 * the presenter bound with an event code.
 * <p>
 * Writing a scope is drawing it, and that is where a scope that left is noticed: each view keeps the views it referred
 * to the last time it was written, and lets go of those it no longer refers to — with the views they referred to in
 * turn. Nothing else releases a view; the presenters do not take part.
 */
public class RemoteViewImpl {

    protected final RemoteApplication app;
    protected final Scope scope;
    protected final CubeSkeleton skeleton;
    protected final String instanceId;

    /** What the scope is told to call on a change: kept to take back only this one. */
    private final Runnable updater = this::update;

    /** The views this one referred to the last time it was written. */
    private Set<RemoteViewImpl> children = Set.of();

    /** The views whose last writing referred to this one. */
    private final Set<RemoteViewImpl> holders = new HashSet<>();

    private boolean released;

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
        this.scope.setUpdater(this.updater);
        this.app.putView(this);
        this.app.markDirty(this);
    }

    /**
     * The remote host's view for any scope: what a client knows it by is the {@code CLASS_ID} the scope class
     * declares (a {@code public static final String}).
     */
    public static RemoteViewImpl forScope(RemoteApplication app, Scope scope) {
        return new RemoteViewImpl(app, scope, classIdOf(scope.getClass()));
    }

    private static String classIdOf(Class<?> scopeClass) {
        try {
            var field = scopeClass.getField("CLASS_ID");
            return (String) field.get(null);
        } catch (ReflectiveOperationException | ClassCastException caught) {
            throw new IllegalStateException(scopeClass.getName() + " declares no public static String CLASS_ID", caught);
        }
    }

    public String instanceId() {
        return this.instanceId;
    }

    public Scope scope() {
        return this.scope;
    }

    public boolean isReleased() {
        return this.released;
    }

    /** The scope changed: the client needs it again. */
    public void update() {
        this.app.markDirty(this);
    }

    /**
     * Lets go of this view: the scope no longer tells it of changes, the client is told to drop it, and so are the
     * views it referred to that no other view refers to.
     */
    public void release() {
        if (this.released) {
            return;
        }
        this.released = true;
        this.scope.clearUpdater(this.updater);
        this.app.forgetView(this);
        this.app.removeView(this.instanceId);
        var gone = this.children;
        this.children = Set.of();
        for (var child : gone) {
            child.dropHolder(this);
        }
    }

    private void dropHolder(RemoteViewImpl holder) {
        this.holders.remove(holder);
        if (this.holders.isEmpty()) {
            this.release();
        }
    }

    public void syncClientToServer(Map<String, Object> formData) {
        this.skeleton.syncScope(formData);
    }

    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        this.skeleton.submit(eventCode, eventQtde, formData);
    }

    /** Writes the scope for the client; the views it no longer refers to are released. */
    public void writeScope(ExtensibleObjectOutput json) {
        var written = new LinkedHashSet<RemoteViewImpl>();
        new ScopeSerializer(json, inner -> {
            var child = this.app.viewOf(inner);
            child.holders.add(this);
            written.add(child);
            return child.instanceId();
        }).write(this.scope, this.instanceId);

        var before = this.children;
        this.children = written;
        for (var child : before) {
            if (!written.contains(child)) {
                child.dropHolder(this);
            }
        }
    }
}
