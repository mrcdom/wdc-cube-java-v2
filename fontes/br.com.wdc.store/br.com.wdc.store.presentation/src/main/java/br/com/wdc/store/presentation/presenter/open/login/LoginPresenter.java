package br.com.wdc.store.presentation.presenter.open.login;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.StorageKeys;
import br.com.wdc.store.presentation.presenter.open.login.calls.CredentialsCheck;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

public class LoginPresenter extends AbstractCubePresenter<StoreApplication> {

    // :: Private Static

    private static final Log LOG = Log.getLogger(LoginPresenter.class);
    private static volatile boolean simulateSlowLogin;

    // :: Public Static

    public static void simulateSlowLogin(boolean value) {
        simulateSlowLogin = value;
    }

    // :: Public Instance Fields

    public final LoginScope scope = bind(new LoginScope());

    // :: Internal Instance Fields

    private ScopeSlot ownerSlot;

    // :: Constructor

    public LoginPresenter(StoreApplication app) {
        super(app);
        this.scope.onEnter = actionOf2(1, FormParam.asString("p.userName"),
                FormParam.of("p.password", v -> app.b64Decipher(CoerceUtils.asString(v))), this::enter);
        this.scope.onSync(formData -> {
            // The client already shows what it sends: no redraw to echo it back
            if (formData.containsKey("userName")) {
                scope.userName.setSilently(CoerceUtils.asString(formData.get("userName")));
            }
        });
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            ownerSlot = new LoginKeys(app, intent).parentSlot();
        }

        ownerSlot.setScope(this.scope);

        return true;
    }

    // :: Messages

    private void alertUserOrPasswordNotRecognize() {
        scope.errorCode.set(1);
        scope.errorMessage.set("Username or password not recognized!");
    }

    private void alertDatabaseIsOffline() {
        scope.errorCode.set(4);
        scope.errorMessage.set("The database is offline!");
    }

    private void alertAccessDenied(AccessDeniedException caught) {
        scope.errorCode.set(6);
        scope.errorMessage.set("Access denied: " + caught.getMessage());

        LOG.warn(scope.errorMessage.get());
    }

    private void alertTooManySignIns(TooManyRequestsException caught) {
        scope.errorCode.set(7);
        var seconds = caught.retryAfterSeconds();
        scope.errorMessage.set(seconds != null
                ? "Too many failed sign-ins. Try again in " + seconds + (seconds == 1 ? " second." : " seconds.")
                : "Too many failed sign-ins. Try again later.");
    }

    private void alertConnectionError(Throwable caught) {
        scope.errorCode.set(5);
        scope.errorMessage.set("Could not reach the server. Check your connection.");

        LOG.error(scope.errorMessage.get(), caught);
    }

    // :: User Actions

    private void enter(String userName, String password) {
        scope.userName.set(userName);
        scope.loading.set(true);
        scope.errorCode.set(0);
        scope.errorMessage.set(null);

        if (simulateSlowLogin) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            var result = new CredentialsCheck()
                    .withUserName(userName)
                    .withPassword(password)
                    .run();

            if (result == null || result.subject() == null || result.subject().getId() == null) {
                app.setSubject(null);
                this.alertUserOrPasswordNotRecognize();
            } else {
                if (result.persistentToken() != null) {
                    app.clientPersistentStore().secure().set(StorageKeys.AUTH_TOKEN, result.persistentToken());
                }
                app.setSubject(result.subject());
                new HomeKeys(app).flip();
            }
        } catch (Exception caught) {
            if (caught instanceof OfflineException) {
                this.alertDatabaseIsOffline();
                return;
            }
            if (caught instanceof TooManyRequestsException tmr) {
                app.setSubject(null);
                this.alertTooManySignIns(tmr);
                return;
            }
            if (caught instanceof AccessDeniedException ade) {
                app.setSubject(null);
                this.alertAccessDenied(ade);
                return;
            }

            LOG.error("enter", caught);
            this.alertConnectionError(caught);
        } finally {
            scope.loading.set(false);
        }
    }
}
