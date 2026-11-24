package br.com.wdc.store.presentation.presenter.open.login.calls;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.PasswordUtil;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.presentation.StoreCall;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;

/** Authenticates the user; {@code null} if the credentials are invalid. */
public class LoginCall extends StoreCall<LoginCall.Result> {

    /**
     * Result of a successful authentication.
     *
     * @param subject         data of the authenticated user
     * @param persistentToken auto-login token (remember me); {@code null} on REST clients
     */
    public record Result(Subject subject, String persistentToken) {
    }

    private String userName;
    private String password;

    public LoginCall withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public LoginCall withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public Result run() {
        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            return authenticateViaAuthService(authService);
        }
        return authenticateViaRepository();
    }

    // :: Internal

    private Result authenticateViaAuthService(AuthenticationService authService) {
        // 1. Password hash (MD5 → base36, same format as the database)
        var passwordHash = PasswordUtil.hashPassword(password);

        // 2. Get the challenge (single-use nonce)
        var challenge = authService.challenge();

        // 3. Compute HMAC-SHA256(key=passwordHash, data=userName+nonce)
        var digest = PasswordUtil.computeHmac(passwordHash, userName + challenge.nonce());

        // 4. Authenticate
        var authResult = authService.login(userName, digest, challenge.nonce());
        if (authResult == null) {
            return null;
        }

        // 5. Persistent token for auto-login (remember me)
        // REST clients manage the session through a refresh token; throws UnsupportedOperationException.
        String persistentToken = null;
        try {
            persistentToken = authService.createPersistentToken(authResult.userId(), userName);
        } catch (UnsupportedOperationException ignored) {
            // REST client: no persistent token
        }

        // 6. Fetch the user's display name
        var users = userRepository().fetch(new UserCriteria()
                .withUserId(authResult.userId())
                .withProjection(Subject.projection()), 0, 1);
        var subject = users.isEmpty() ? null : Subject.create(users.get(0));

        return new Result(subject, persistentToken);
    }

    /**
     * Fallback for environments without security (unit tests without initializeSecurity).
     *
     * <p>
     * Looks up by login and <b>compares the digest here</b>, instead of sending it as a criterion. The password is not
     * a searchable field: leaving it on the criteria surface would also expose it on the REST API, where filtering users
     * by password serves no one. It is the same path {@code AuthenticationServiceImpl} follows in the real flow — it
     * projects the digest and checks it — only without the challenge, which does not exist here.
     * </p>
     */
    private Result authenticateViaRepository() {
        var projection = Subject.projection()
                .withPassword(ProjectionValues.INSTANCE.str);

        var users = userRepository().fetch(new UserCriteria()
                .withUserName(userName)
                .withProjection(projection), 0, 1);

        if (users.isEmpty()) {
            return null;
        }

        var user = users.get(0);
        // The column is CHAR and comes right-padded with spaces; without the trim the comparison would never match.
        var stored = user.password() == null ? null : user.password().trim();
        if (stored == null || !stored.equals(PasswordUtil.hashPassword(password))) {
            return null;
        }

        return new Result(Subject.create(user), null);
    }

}
