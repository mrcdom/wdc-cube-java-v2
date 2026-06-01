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
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.PresenterBase;
import br.com.wdc.framework.cube.ViewState;

/**
 * Generic browser presenter: represents the top-level browser view
 * that wraps the application's root presenter content.
 * <p>
 * Handles: alert dialogs, navigation start, history changes, keep-alive.
 */
public class RemoteBrowserPresenter implements PresenterBase {

    public static class BrowserState implements ViewState {
        public String instanceId;
        public int alertId;
        public List<String> alertArgs = Collections.emptyList();
        public CubeView contentView;
    }

    public final RemoteApplication app;
    public final BrowserState state = new BrowserState();

    private final CubeSkeleton skeleton = skeleton();
    private RemoteViewImpl view;
    private ThrowingRunnable alertAction = ThrowingRunnable.noop();

    public RemoteBrowserPresenter(RemoteApplication app) {
        this.app = app;
        this.view = new RemoteViewImpl(app, this, this.state, this.skeleton, this.skeleton.classId() + ":0");
        this.state.instanceId = this.view.instanceId();
    }

    @Override
    public void commitComputedState() {
        var rootPresenter = this.app.getRootPresenter();
        CubeView rootView = null;
        if (rootPresenter instanceof AbstractCubePresenter<?> acp) {
            rootView = acp.view();
        }
        if (this.state.contentView != rootView) {
            this.state.contentView = rootView;
        }
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

        this.state.alertId = code;
        this.state.alertArgs = Collections.emptyList();

        if (args != null && args.length > 0) {
            this.state.alertArgs = new ArrayList<>(args.length);
            for (Object arg : args) {
                this.state.alertArgs.add(String.valueOf(arg));
            }
        }
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
            this.state.alertId = 0;
            this.state.alertArgs = Collections.emptyList();
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
