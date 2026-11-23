package br.com.wdc.shopping.presentation.presenter;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.StorageKeys;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

public class RootPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Public Class Fields

    private static final Log LOG = Log.getLogger(RootPresenter.class);

    public static Function<RootPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class RootViewState extends ViewState {

        public final Prop<CubeView> contentView = prop();
        public final Prop<String> errorMessage = prop();

    }

    public final RootViewState state = bind(new RootViewState());

    // :: Internal Instance Fields

    private final CubeViewSlot contentSlot;

    // :: Constructor

    public RootPresenter(ShoppingApplication app) {
        super(app);
        this.contentSlot = this::setContentView;
    }

    // :: Cube API

    @Override
    public void release() {
        this.state.contentView.set(null);
        super.release();
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            this.view = createView.apply(this);
            tryAutoLogin();
        }

        if (deepest) {
            // The root has no screen of its own: it goes on to the home (which sends to the login when there is no user)
            new HomeKeys(this.app, intent).flip();
            return false;
        } else {
            // Does not accept changing user id at URL
            var keys = new RootKeys(this.app, intent);
            if (this.app.getSubject() != null) {
                keys.withUserId(this.app.getSubject().getId());
            }
            keys.withParentSlot(this.contentSlot);
        }

        return true;
    }

    /**
     * Attempts auto-login using a persistent token stored as app attribute.
     * Called once during initialization. If the token is valid, sets Subject
     * and SecurityContext so the user skips the login screen.
     */
    private void tryAutoLogin() {
        var token = this.app.clientPersistentStore().secure().get(StorageKeys.AUTH_TOKEN);
        if (StringUtils.isBlank(token)) {
            return;
        }

        var authService = AuthenticationService.BEAN.get();
        if (authService == null) {
            return;
        }

        var authResult = authService.loginWithPersistentToken(token);
        if (authResult == null) {
            LOG.info("Auto-login failed: invalid or expired token");
            this.app.clientPersistentStore().secure().remove(StorageKeys.AUTH_TOKEN);
            return;
        }

        // Resolve SecurityContext
        var securityContext = authService.resolveToken(authResult.accessToken());
        if (securityContext != null) {
            SecurityContext.CURRENT.set(securityContext);
            this.app.setSecurityContext(securityContext);
        }

        // Fetch user and set Subject
        var users = this.app.getUserRepository().fetch(
                new UserCriteria().withUserId(authResult.userId())
                        .withProjection(Subject.projection()),
                0, 1);
        if (!users.isEmpty()) {
            this.app.setSubject(Subject.create(users.get(0)));
            LOG.info("Auto-login successful for user: {}",
                    securityContext != null ? securityContext.userName() : authResult.userId());
        }
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        if (this.app.getSubject() != null) {
            new RootKeys(this.app, intent).withUserId(this.app.getSubject().getId());
        }
    }

    // :: Messages

    public void alertUnexpectedError(String message, Exception caught) {
        this.alertUnexpectedError(LOG, message, caught);
    }

    public void alertUnexpectedError(Log logger, String message, Throwable caught) {
        if (StringUtils.isBlank(caught.getMessage())) {
            this.state.errorMessage.set(message);
        } else {
            this.state.errorMessage.set(message + ": " + caught.getMessage());
        }
        // The same message again is still a new alert: the prop alone would not redraw
        this.update();
        if (logger != null) {
            logger.error(this.state.errorMessage.get(), caught);
        } else {
            LOG.error(this.state.errorMessage.get(), caught);
        }
    }

    // :: Slots

    private void setContentView(CubeView view) {
        this.state.contentView.set(view);
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return skeleton("f2d345c4a610");
    }

}
