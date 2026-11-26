package br.com.wdc.store.presentation.presenter.open.login.calls;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.resource.ResourceCall;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.Scram;
import br.com.wdc.framework.domain.security.ScramVerifier;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.presentation.presenter.open.login.structs.Subject;

/**
 * Checks a user name and password, and gives who it is: the login. Not a read nor a write of a resource, so a verb of
 * its own on {@link ResourceCall}.
 * <p>
 * With the authentication service (security on) it is a SCRAM-SHA-256 exchange ({@link Scram}): the password never
 * leaves this process, and the server must prove it holds the user's verifier. Without it, the password is checked
 * against the stored verifier here. {@code null} when the credentials are wrong; too many failures for the user make it
 * wait ({@link br.com.wdc.framework.domain.exception.TooManyRequestsException}).
 */
public class CredentialsCheck extends ResourceCall<CredentialsCheck.Result> {

    /**
     * Result of a successful authentication.
     *
     * @param subject         data of the authenticated user
     * @param persistentToken auto-login token (remember me); {@code null} when the authentication service keeps the session itself
     */
    public record Result(Subject subject, String persistentToken) {
    }

    private String userName;
    private String password;

    public CredentialsCheck withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public CredentialsCheck withPassword(String password) {
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
        var challenge = authService.challenge(userName);
        var response = Scram.current().respond(userName, password, challenge);
        var authResult = authService.login(userName, response.proof(), challenge.nonce());
        if (authResult == null) {
            return null;
        }

        if (!response.serverIsAuthentic(authResult.serverSignature())) {
            authService.logout(authResult.refreshToken());
            throw new BusinessException("The server could not prove it holds the user's credentials");
        }

        // Persistent token for auto-login (remember me)
        // An authentication service that keeps the session in a refresh token of its own (teavm.web's) throws
        // UnsupportedOperationException; the JVM REST client answers with its refresh token.
        String persistentToken = null;
        try {
            persistentToken = authService.createPersistentToken(authResult.userId(), userName);
        } catch (UnsupportedOperationException ignored) {
            // the service keeps the session itself: no persistent token
        }

        // The user's display name
        var users = UserRepository.BEAN.get().fetch(new UserCriteria()
                .withUserId(authResult.userId())
                .withProjection(Subject.projection()), 0, 1);
        var subject = users.isEmpty() ? null : Subject.create(users.get(0));

        return new Result(subject, persistentToken);
    }

    /**
     * Without an authentication service (tests and local use without security): the password is not a criteria field,
     * so it searches by login only, projecting the stored verifier, and checks the password against it — the same check
     * the authentication service makes on the server, only without the exchange.
     */
    private Result authenticateViaRepository() {
        var projection = Subject.projection()
                .withPassword(ProjectionValues.INSTANCE.str);

        var users = UserRepository.BEAN.get().fetch(new UserCriteria()
                .withUserName(userName)
                .withProjection(projection), 0, 1);

        if (users.isEmpty()) {
            return null;
        }

        var user = users.get(0);
        var verifier = ScramVerifier.parse(user.password());
        if (verifier == null || !Scram.current().matches(verifier, password)) {
            return null;
        }

        return new Result(Subject.create(user), null);
    }

}
