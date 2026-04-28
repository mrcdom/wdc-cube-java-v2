package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class BrowserReactViewImpl extends GenericViewImpl {

    public static final String VID = "7b32e816a191";
    public static final String VSID = VID + ":0";

    private List<String> alertArgs = Collections.emptyList();
    private ThrowingRunnable alertAction = ThrowingRunnable.noop();

    public BrowserReactViewImpl(ApplicationReactImpl app) {
        super(app, VID, 0);
    }

    public void alertUnexpectedError(String msg, Throwable cause) {
        int alertCode = -1;
        if (msg == null) {
            if (cause != null) {
                msg = cause.getMessage();
            } else {
                msg = "Ocorreu um erro não previsto";
            }
        }

        if (cause != null) {
            var detail = ExceptionUtils.getStackTrace(cause);
            this.alert(ThrowingRunnable.noop(), alertCode, msg, detail);
        } else {
            this.alert(ThrowingRunnable.noop(), alertCode, msg);
        }
    }

    public void alert(ThrowingRunnable action, int code, Object... args) {
        var oldAlertAction = this.alertAction;
        this.alertAction = () -> {
            oldAlertAction.runThrows();
            action.runThrows();
        };

        this.alertId = code;
        this.alertArgs = Collections.emptyList();

        if (args != null && args.length > 0) {
            this.alertArgs = new ArrayList<>(args.length);
            for (Object arg : args) {
                this.alertArgs.add(String.valueOf(arg));
            }
        }
        this.update();
    }

    private void onStart(String path) throws Exception {
        app.safeGo(path);
        app.updateAllViews();
    }

    private void onHistoryChanged(String path) {
        try {
            app.safeGo(path);
        } catch (Exception e) {
            var logger = LoggerFactory.getLogger(this.getClass());
            logger.warn("onHistoryChanged", e);
        }
    }

    private boolean onAlertOk() {
        try {
            this.alertId = 0;
            this.alertArgs = Collections.emptyList();
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

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> this.onAlertOk();
        case 2 -> this.onKeepAlive();
        case -1 -> {
            try {
                var path = CoerceUtils.asString(formData.get("p.path"));
                this.onStart(path);
            } catch (Exception e) {
                var logger = LoggerFactory.getLogger(this.getClass());
                logger.warn("onStart", e);
            }
        }
        case -2 -> {
            var path = CoerceUtils.asString(formData.get("p.path"));
            this.onHistoryChanged(path);
        }
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        json.beginObject();
        {
            json.name("id").value(this.instanceId);

            if (alertId != 0) {
                json.name("alertMessage").beginObject();
                {
                    json.name("id").value(alertId);
                    json.name("args").beginArray();
                    {
                        alertArgs.forEach(json::value);
                    }
                    json.endArray();
                }
                json.endObject();
            }

            if (this.app.getRootPresenter() != null
                    && this.app.getRootPresenter().view() instanceof GenericViewImpl view) {
                json.name("contentViewId").value(view.instanceId());
            }
        }
        json.endObject();
    }

}
