package br.com.wdc.framework.cube.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;

/**
 * Generic browser presenter: represents the top-level browser view
 * that wraps the application's root presenter content.
 * <p>
 * Handles: alert dialogs, navigation start, history changes, keep-alive.
 */
public class RemoteBrowserPresenter implements PresenterBase {

    public static class BrowserScope extends Scope {
        public final Prop<String> instanceId = prop();
        public final Prop<Integer> alertId = prop(0);
        public final Prop<List<String>> alertArgs = prop(Collections.emptyList());
        /** The root presenter's scope: drawn by the view the catalog gives it, made when this state is written. */
        public final Prop<Scope> content = prop();
    }

    public final RemoteApplication app;
    public final BrowserScope state = new BrowserScope();

    private final CubeSkeleton skeleton = skeleton();
    private RemoteViewImpl view;
    private ThrowingRunnable alertAction = ThrowingRunnable.noop();

    public RemoteBrowserPresenter(RemoteApplication app) {
        this.app = app;
        this.view = new RemoteViewImpl(app, this, this.state, this.skeleton, this.skeleton.classId() + ":0");
        this.state.setUpdater(this::update);
        this.state.instanceId.set(this.view.instanceId());
    }

    @Override
    public void commitComputedState() {
        var rootPresenter = this.app.getRootPresenter();
        this.state.content.set(rootPresenter instanceof AbstractCubePresenter<?> acp ? acp.scope() : null);
    }

    @Override
    public void release() {
        this.view.release();
    }

    public RemoteViewImpl getView() {
        return view;
    }

    public void update() {
        this.app.markDirty(this.view);
    }

    public void alertUnexpectedError(Log log, String msg, Throwable cause) {
        int alertCode = -1;
        if (msg == null) {
            if (cause != null) {
                msg = cause.getMessage();
            } else {
                msg = "Ocorreu um erro não previsto";
            }
        }

        if (cause != null) {
        	log.error(msg, cause);
            var detail = ExceptionUtils.getStackTrace(cause);
            this.alert(ThrowingRunnable.noop(), alertCode, msg, detail);
        } else {
        	log.error(msg);
            this.alert(ThrowingRunnable.noop(), alertCode, msg);
        }
    }

    public void alert(ThrowingRunnable action, int code, Object... args) {
        var oldAlertAction = this.alertAction;
        this.alertAction = () -> {
            oldAlertAction.runThrows();
            action.runThrows();
        };

        var alertArgs = new ArrayList<String>(args != null ? args.length : 0);
        if (args != null) {
            for (Object arg : args) {
                alertArgs.add(String.valueOf(arg));
            }
        }
        this.state.alertId.set(code);
        this.state.alertArgs.set(alertArgs);
        // The same alert again is still a new alert
        this.update();
    }

    private void onStart(String path) {
        app.safeGo(path);
        app.updateAllViews();
    }

    private void onHistoryChanged(String path) {
        try {
            app.safeGo(path);
        } catch (Exception e) {
            var logger = Log.getLogger(this.getClass());
            logger.warn("onHistoryChanged", e);
        }
    }

    private boolean onAlertOk() {
        try {
            this.state.alertId.set(0);
            this.state.alertArgs.set(Collections.emptyList());
            this.alertAction.run();
        } finally {
            this.alertAction = ThrowingRunnable.noop();
            this.update();
        }
        return true;
    }

    private boolean onKeepAlive() {
        app.extendLife();
        return true;
    }

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "7b32e816a191";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                switch (eventCode) {
                case 1 -> onAlertOk();
                case 2 -> onKeepAlive();
                case -1 -> {
                    try {
                        var path = CoerceUtils.asString(formData.get("p.path"));
                        onStart(path);
                    } catch (Exception e) {
                        var logger = Log.getLogger(this.getClass());
                        logger.warn("onStart", e);
                    }
                }
                case -2 -> {
                    var path = CoerceUtils.asString(formData.get("p.path"));
                    onHistoryChanged(path);
                }
                default -> new AssertionError("eventCode(" + eventCode + ") not handled");
                }
            }
        };
    }
}
