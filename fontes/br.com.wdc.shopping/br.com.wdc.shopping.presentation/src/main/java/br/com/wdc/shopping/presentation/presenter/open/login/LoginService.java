package br.com.wdc.shopping.presentation.presenter.open.login;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.PasswordUtil;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

public class LoginService {

    private final ShoppingApplication app;

    public LoginService(ShoppingApplication app) {
        this.app = app;
    }

    public Subject fetchSubject(String userName, String password) {
        var authService = AuthenticationService.BEAN.get();
        if (authService != null && app != null) {
            return authenticateViaAuthService(authService, userName, password);
        }
        return authenticateViaRepository(userName, password);
    }

    private Subject authenticateViaAuthService(AuthenticationService authService, String userName, String password) {
        // 1. Hash da senha (MD5 → base36, mesmo formato do banco)
        var passwordHash = PasswordUtil.hashPassword(password);

        // 2. Obter challenge (nonce de uso único)
        var challenge = authService.challenge();

        // 3. Calcular HMAC-SHA256(key=passwordHash, data=userName+nonce)
        var digest = PasswordUtil.computeHmac(passwordHash, userName + challenge.nonce());

        // 4. Autenticar
        var authResult = authService.login(userName, digest, challenge.nonce());
        if (authResult == null) {
            return null;
        }

        // 5. Resolver token → SecurityContext (server-side; null em REST client)
        var securityContext = authService.resolveToken(authResult.accessToken());
        if (securityContext != null) {
            SecurityContextHolder.set(securityContext);
        }

        // 6. Armazenar SecurityContext na aplicação (para delegates de repositório)
        app.setSecurityContext(securityContext);

        // 7. Buscar nome de exibição do usuário
        var users = app.getUserRepository().fetch(new UserCriteria()
                .withUserId(authResult.userId())
                .withProjection(Subject.projection())
                .withLimit(1));

        return users.isEmpty() ? null : Subject.create(users.get(0));
    }

    /**
     * Fallback para ambientes sem segurança (testes unitários sem initializeSecurity).
     */
    private Subject authenticateViaRepository(String userName, String password) {
        var repository = UserRepository.BEAN.get();
        return repository.fetch(new UserCriteria()
                .withUserName(userName)
                .withPassword(password)
                .withProjection(Subject.projection())
                .withLimit(1))
                .stream().map(Subject::create)
                .findFirst().orElse(null);
    }

}