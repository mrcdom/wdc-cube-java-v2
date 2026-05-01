package br.com.wdc.shopping.presentation.presenter.open.login;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.shopping.domain.exception.OfflineException;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.Routes;

public class LoginPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Private Class Fields

    private static final Logger LOG = LoggerFactory.getLogger(LoginPresenter.class);

    // :: Public Static Fields

    public static Function<LoginPresenter, CubeView> createView;

    // :: Public Instance Fields

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

        return false;
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

    // :: User Actions

    public void onEnter() {
        try {
            var subject = loginService.fetchSubject(state.userName, state.password);

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
            app.alertUnexpectedError(LOG, "Trying to access restricted area", caught);
        }
    }
}
