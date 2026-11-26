package br.com.wdc.framework.cube.remote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.AbstractPresenter;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;

/**
 * Generic browser presenter: represents the top-level browser view that wraps the application's root presenter
 * content.
 * <p>
 * Handles: alert dialogs, navigation start, history changes, keep-alive. Its view is the client's entry point, with
 * the one instance id every client knows ({@link #BROWSER_VSID}); its scope holds the root presenter's scope, whose
 * view is made when the browser's scope is written.
 */
public class RemoteBrowserPresenter extends AbstractPresenter<CubeApplication> {

    private static final Log LOG = Log.getLogger(RemoteBrowserPresenter.class);

    public static class BrowserScope extends Scope {

        public static final String CLASS_ID = "7b32e816a191";

        public final Prop<String> instanceId = prop();
        public final Prop<Integer> alertId = prop(0);
        public final Prop<List<String>> alertArgs = prop(Collections.emptyList());
        /** The root presenter's scope. */
        public final Prop<Scope> content = prop();

        public Action onAlertOk = Action.NOOP;
        public Action onKeepAlive = Action.NOOP;
        public ActionOf<String> onStart = ActionOf.noop();
        public ActionOf<String> onHistoryChanged = ActionOf.noop();

    }

    /** The browser view's instance id: fixed, the first thing a client talks to. */
    public static final String BROWSER_VSID = BrowserScope.CLASS_ID + ":0";

    public final RemoteApplication remoteApp;
    public final BrowserScope scope = bind(new BrowserScope());

    private final RemoteViewImpl view;
    private ThrowingRunnable alertAction = ThrowingRunnable.noop();

    /** @param app the remote application, which is also the {@link CubeApplication} */
    public RemoteBrowserPresenter(RemoteApplication app) {
        super((CubeApplication) app);
        this.remoteApp = app;

        this.scope.onAlertOk = action(1, this::alertOk);
        this.scope.onKeepAlive = action(2, app::extendLife);
        this.scope.onStart = actionOf(-1, FormParam.asString("p.path"), this::start);
        this.scope.onHistoryChanged = actionOf(-2, FormParam.asString("p.path"), this::historyChanged);

        // The entry point: drawn by its own view, with the fixed id, not through the catalog
        this.view = new RemoteViewImpl(app, this.scope, BrowserScope.CLASS_ID, BROWSER_VSID);
        this.scope.instanceId.set(this.view.instanceId());
    }

    @Override
    public void commitComputedState() {
        var rootPresenter = this.remoteApp.getRootPresenter();
        this.scope.content.set(rootPresenter instanceof AbstractCubePresenter<?> acp ? acp.scope() : null);
    }

    /** Lets go of the client's views: the browser's, and with it every view it refers to. */
    @Override
    public void release() {
        this.view.release();
    }

    public RemoteViewImpl getView() {
        return view;
    }

    // :: Alerts

    /**
     * Shows the client the message of an unexpected error: what failed and the cause's message, as the local hosts
     * do. The stack trace stays in the server's log.
     */
    public void alertUnexpectedError(Log log, String msg, Throwable cause) {
        var causeMessage = cause != null ? cause.getMessage() : null;
        String text;
        if (StringUtils.isBlank(msg)) {
            text = StringUtils.isBlank(causeMessage) ? "An unexpected error occurred" : causeMessage;
        } else {
            text = StringUtils.isBlank(causeMessage) ? msg : msg + ": " + causeMessage;
        }

        if (cause != null) {
            log.error(text, cause);
        } else {
            log.error(text);
        }
        this.alert(ThrowingRunnable.noop(), -1, text);
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
        this.scope.alertId.set(code);
        this.scope.alertArgs.set(alertArgs);
        // The same alert again is still a new alert
        this.update();
    }

    // :: Actions

    private void alertOk() throws Exception {
        try {
            this.scope.alertId.set(0);
            this.scope.alertArgs.set(Collections.emptyList());
            this.alertAction.runThrows();
        } finally {
            this.alertAction = ThrowingRunnable.noop();
            this.update();
        }
    }

    private void start(String path) {
        try {
            this.remoteApp.safeGo(path);
        } catch (Exception caught) {
            LOG.warn("onStart", caught);
        }
        this.remoteApp.updateAllViews();
    }

    private void historyChanged(String path) {
        try {
            this.remoteApp.safeGo(path);
        } catch (Exception caught) {
            LOG.warn("onHistoryChanged", caught);
        }
    }
}
