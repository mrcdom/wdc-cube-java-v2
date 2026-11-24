package br.com.wdc.store.presentation.presenter;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.framework.cube.ScopeSlot;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.StorageKeys;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;

public class RootPresenter extends AbstractCubePresenter<StoreApplication> {

    // :: Public Class Fields

    private static final Log LOG = Log.getLogger(RootPresenter.class);

    // :: Public Instance Fields

    public final RootScope scope = bind(new RootScope());

    // :: Internal Instance Fields

    private final ScopeSlot contentSlot;

    // :: Constructor

    public RootPresenter(StoreApplication app) {
        super(app);
        this.contentSlot = this::setContent;
    }

    // :: Cube API

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        if (initialization) {
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
     * Called once during initialization. If the token is valid, sets the Subject
     * so the user skips the login screen.
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

        // Fetch user and set Subject
        var users = UserRepository.BEAN.get().fetch(
                new UserCriteria()
                        .withUserId(authResult.userId())
                        .withProjection(Subject.projection()),
                0, 1);
        if (!users.isEmpty()) {
            this.app.setSubject(Subject.create(users.get(0)));
            LOG.info("Auto-login successful for user: {}", authResult.userId());
        }
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        if (this.app.getSubject() != null) {
            new RootKeys(this.app, intent)
                    .withUserId(this.app.getSubject().getId());
        }
    }

    // :: Messages

    public void alertUnexpectedError(String message, Exception caught) {
        this.alertUnexpectedError(LOG, message, caught);
    }

    public void alertUnexpectedError(Log logger, String message, Throwable caught) {
        if (StringUtils.isBlank(caught.getMessage())) {
            this.scope.errorMessage.set(message);
        } else {
            this.scope.errorMessage.set(message + ": " + caught.getMessage());
        }
        // The same message again is still a new alert: the prop alone would not redraw
        this.update();
        if (logger != null) {
            logger.error(this.scope.errorMessage.get(), caught);
        } else {
            LOG.error(this.scope.errorMessage.get(), caught);
        }
    }

    // :: Slots

    private void setContent(Scope content) {
        this.scope.content.set(content);
    }

}
