package br.com.wdc.shopping.presentation.presenter.open.login;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.PasswordUtil;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

public class LoginService {

    private final UserRepository userRepository;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Autentica o usuário e retorna o resultado completo.
     * Retorna {@code null} se as credenciais forem inválidas.
     */
    public LoginResult fetchSubject(String userName, String password) {
        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            return authenticateViaAuthService(authService, userName, password);
        }
        return authenticateViaRepository(userName, password);
    }

    private LoginResult authenticateViaAuthService(AuthenticationService authService, String userName, String password) {
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
        SecurityContext securityContext = null;
        try {
            securityContext = authService.resolveToken(authResult.accessToken());
        } catch (UnsupportedOperationException ignored) {
            // REST clients não resolvem JWT localmente; toda autorização é
            // delegada ao servidor via Bearer token.
        }
        if (securityContext != null) {
            SecurityContext.CURRENT.set(securityContext);
        }

        // 6. Token persistente para auto-login (remember me)
        // REST clients gerenciam sessão via refresh token; lança UnsupportedOperationException.
        String persistentToken = null;
        try {
            persistentToken = authService.createPersistentToken(authResult.userId(), userName);
        } catch (UnsupportedOperationException ignored) {
            // REST client: sem persistent token
        }

        // 7. Buscar nome de exibição do usuário
        var users = userRepository.fetch(new UserCriteria()
                .withUserId(authResult.userId())
                .withProjection(Subject.projection()), 0, 1);
        var subject = users.isEmpty() ? null : Subject.create(users.get(0));

        return new LoginResult(subject, securityContext, persistentToken);
    }

    /**
     * Fallback para ambientes sem segurança (testes unitários sem initializeSecurity).
     */
    private LoginResult authenticateViaRepository(String userName, String password) {
        var subject = userRepository.fetch(new UserCriteria()
                .withUserName(userName)
                .withPassword(password)
                .withProjection(Subject.projection()), 0, 1)
                .stream().map(Subject::create)
                .findFirst().orElse(null);
        return subject != null ? new LoginResult(subject, null, null) : null;
    }

}
