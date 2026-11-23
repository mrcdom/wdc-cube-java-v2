package br.com.wdc.shopping.presentation.presenter.open.login;


import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.ActionOf2;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.FormParam;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.StorageKeys;
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

    public static class LoginScope extends Scope {

        public static final String CLASS_ID = "c677cda52d14";

        public final Prop<String> userName = prop();
        public final Prop<Integer> errorCode = prop(0);
        public final Prop<String> errorMessage = prop();
        public final Prop<Boolean> loading = prop(false);

        public ActionOf2<String, String> onEnter = ActionOf2.noop();

    }

    public final LoginScope state = bind(new LoginScope());

    // :: Internal Instance Fields

    private final LoginService loginService;
    private ScopeSlot ownerSlot;

    // :: Constructor

    public LoginPresenter(ShoppingApplication app) {
        super(app);
        this.loginService = new LoginService(app.getUserRepository());
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
        state.errorMessage.set("Usuário ou senha não reconhecido!");
    }

    private void alertDatabaseIsOffline() {
        state.errorCode.set(4);
        state.errorMessage.set("Banco de dados esta fora do ar!");
    }

    private void alertConnectionError(Throwable caught) {
        state.errorCode.set(5);
        state.errorMessage.set("Falha de comunicação com o servidor. Verifique sua conexão.");

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
            var result = loginService.fetchSubject(userName, password);

            if (result == null || result.subject() == null || result.subject().getId() == null) {
                app.setSubject(null);
                this.alertUserOrPasswordNotRecognize();
            } else {
                if (result.securityContext() != null) {
                    app.setSecurityContext(result.securityContext());
                }
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

            LOG.error("enter", caught);
            this.alertConnectionError(caught);
        } finally {
            state.loading.set(false);
        }
    }
}
