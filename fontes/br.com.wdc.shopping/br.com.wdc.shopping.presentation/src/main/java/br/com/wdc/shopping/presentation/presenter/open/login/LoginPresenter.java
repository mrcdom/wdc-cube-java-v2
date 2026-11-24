package br.com.wdc.shopping.presentation.presenter.open.login;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.StorageKeys;
import br.com.wdc.shopping.presentation.presenter.open.login.calls.LoginCall;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;

public class LoginPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Static

    private static final Log LOG = Log.getLogger(LoginPresenter.class);
    private static volatile boolean simulateSlowLogin;

    // :: Public Static

    public static void simulateSlowLogin(boolean value) {
        simulateSlowLogin = value;
    }

    // :: Public Instance Fields

    public final LoginScope state = bind(new LoginScope());

    // :: Internal Instance Fields

    private ScopeSlot ownerSlot;

    // :: Constructor

    public LoginPresenter(ShoppingApplication app) {
        super(app);
        this.state.onEnter = actionOf2(1, FormParam.asString("p.userName"),
                FormParam.of("p.password", v -> app.b64Decipher(CoerceUtils.asString(v))), this::enter);
        this.state.onSync(formData -> {
            // The client already shows what it sends: no redraw to echo it back
            if (formData.containsKey("userName")) {
                state.userName.setSilently(CoerceUtils.asString(formData.get("userName")));
            }
        });
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            ownerSlot = new LoginKeys(app, intent).parentSlot();
        }

        ownerSlot.setScope(this.state);

        return true;
    }

    // :: Messages

    private void alertUserOrPasswordNotRecognize() {
        state.errorCode.set(1);
        state.errorMessage.set("Username or password not recognized!");
    }

    private void alertDatabaseIsOffline() {
        state.errorCode.set(4);
        state.errorMessage.set("The database is offline!");
    }

    private void alertAccessDenied(AccessDeniedException caught) {
        state.errorCode.set(6);
        state.errorMessage.set("Access denied: " + caught.getMessage());

        LOG.warn(state.errorMessage.get());
    }

    private void alertConnectionError(Throwable caught) {
        state.errorCode.set(5);
        state.errorMessage.set("Could not reach the server. Check your connection.");

        LOG.error(state.errorMessage.get(), caught);
    }

    // :: User Actions

    private void enter(String userName, String password) {
        state.userName.set(userName);
        state.loading.set(true);
        state.errorCode.set(0);
        state.errorMessage.set(null);

        if (simulateSlowLogin) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            var result = new LoginCall()
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
            if (caught instanceof AccessDeniedException ade) {
                app.setSubject(null);
                this.alertAccessDenied(ade);
                return;
            }

            LOG.error("enter", caught);
            this.alertConnectionError(caught);
        } finally {
            state.loading.set(false);
        }
    }
}
