package br.com.wdc.shopping.presentation.presenter.open.login;

import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class LoginPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Static

    private static final Log LOG = Log.getLogger(LoginPresenter.class);
    private static volatile boolean simulateSlowLogin;

    // :: Public Static

    public static Function<LoginPresenter, CubeView> createView;

    public static void simulateSlowLogin(boolean value) {
        simulateSlowLogin = value;
    }

    // :: Public Instance Fields

    public static class LoginViewState implements ViewState {

        public String userName;
        public int errorCode;
        public String errorMessage;
        public boolean loading;

    }

    public final LoginViewState state = new LoginViewState();

    // :: Internal Instance Fields

    private final LoginService loginService;
    private CubeViewSlot ownerSlot;

    // :: Constructor

    public LoginPresenter(ShoppingApplication app) {
        super(app);
        this.loginService = new LoginService(app);
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            ownerSlot = intent.getViewSlot(PlaceAttributes.SLOT_OWNER);
            this.view = createView.apply(this);
        }

        ownerSlot.setView(view);

        return true;
    }

    // :: Messages

    private void alertUserOrPasswordNotRecognize() {
        state.errorCode = 1;
        state.errorMessage = "Usuário ou senha não reconhecido!";
        this.update();
    }

    private void alertDatabaseIsOffline() {
        state.errorCode = 4;
        state.errorMessage = "Banco de dados esta fora do ar!";
        this.update();
    }

    private void alertConnectionError(Throwable caught) {
        state.errorCode = 5;
        state.errorMessage = "Falha de comunicação com o servidor. Verifique sua conexão.";
        this.update();

        LOG.error(state.errorMessage, caught);
    }

    // :: User Actions

    public void onEnter(String userName, String password) {
        state.userName = userName;
        state.loading = true;
        state.errorCode = 0;
        state.errorMessage = null;
        this.update();

        if (simulateSlowLogin) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            var subject = loginService.fetchSubject(userName, password);

            if (subject == null || subject.getId() == null) {
                app.setSubject(null);
                this.alertUserOrPasswordNotRecognize();
            } else {
                app.setSubject(subject);
                Routes.home(app);
            }
        } catch (Exception caught) {
            if (caught instanceof OfflineException) {
                this.alertDatabaseIsOffline();
                return;
            }

            LOG.error("onEnter", caught);
            this.alertConnectionError(caught);
        } finally {
            state.loading = false;
            this.update();
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "c677cda52d14";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                if (eventCode == 1) {
                    var userName = CoerceUtils.asString(formData.get("p.userName"));
                    var password = app.b64Decipher(CoerceUtils.asString(formData.get("p.password")));
                    onEnter(userName, password);
                }
            }

            @Override
            public void syncState(Map<String, Object> formData) {
                var fn = "userName";
                if (formData.containsKey(fn)) {
                    state.userName = CoerceUtils.asString(formData.get(fn));
                }
            }

        };
    }
}
