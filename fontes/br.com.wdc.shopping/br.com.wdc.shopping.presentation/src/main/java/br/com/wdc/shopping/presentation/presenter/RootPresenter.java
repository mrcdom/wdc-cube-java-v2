package br.com.wdc.shopping.presentation.presenter;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubeSkeleton;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;
import br.com.wdc.framework.cube.ViewState;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.presentation.PlaceAttributes;
import br.com.wdc.shopping.presentation.PlaceParameters;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.exception.WrongPlace;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

public class RootPresenter extends AbstractCubePresenter<ShoppingApplication> {

    // :: Public Class Fields

    private static final Log LOG = Log.getLogger(RootPresenter.class);

    public static Function<RootPresenter, CubeView> createView;

    // :: Public Instance Fields

    public static class RootViewState implements ViewState {

        public CubeView contentView;
        public String errorMessage;

    }

    public final RootViewState state = new RootViewState();

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
        this.state.contentView = null;
        super.release();
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
            this.view = createView.apply(this);
            tryAutoLogin();
        }

        if (deepest) {
            throw new WrongPlace();
        } else {
            // Does not accept changing user id at URL
            if (this.app.getSubject() != null) {
                intent.setParameter(PlaceParameters.USER_ID, this.app.getSubject().getId());
            }
            intent.setViewSlot(PlaceAttributes.SLOT_OWNER, this.contentSlot);
        }

        return true;
    }

    /**
     * Attempts auto-login using a persistent token stored as app attribute.
     * Called once during initialization. If the token is valid, sets Subject
     * and SecurityContext so the user skips the login screen.
     */
    private void tryAutoLogin() {
        var token = (String) this.app.getAttribute("accessToken");
        if (StringUtils.isBlank(token)) {
            return;
        }
        // Remove from in-memory attributes (token remains persisted on client for 30 days)
        this.app.removeAttribute("accessToken");

        var authService = AuthenticationService.BEAN.get();
        if (authService == null) {
            return;
        }

        var authResult = authService.loginWithPersistentToken(token);
        if (authResult == null) {
            LOG.info("Auto-login failed: invalid or expired token");
            this.app.emitAccessToken("");
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
            intent.setParameter(PlaceParameters.USER_ID, this.app.getSubject().getId());
        }
    }

    // :: Messages

    public void alertUnexpectedError(String message, Exception caught) {
        this.alertUnexpectedError(LOG, message, caught);
    }

    public void alertUnexpectedError(Log logger, String message, Throwable caught) {
        if (StringUtils.isNotBlank(caught.getMessage())) {
            this.state.errorMessage = message;
        } else {
            this.state.errorMessage = message + ": " + caught.getMessage();
        }
        this.update();
        if (logger != null) {
            logger.error(this.state.errorMessage, caught);
        } else {
            LOG.error(this.state.errorMessage, caught);
        }
    }

    // :: Slots

    private void setContentView(CubeView view) {
        if (this.state.contentView != view) {
            this.state.contentView = view;
            this.update();
        }
    }

    // :: Controle remoto

    public CubeSkeleton skeleton() {
        return new CubeSkeleton() {

            @Override
            public String classId() {
                return "f2d345c4a610";
            }

            @Override
            public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
                // NOOP
            }
        };
    }

}
