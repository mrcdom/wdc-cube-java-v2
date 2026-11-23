package br.com.wdc.shopping.presentation.presenter.open.login.calls;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.PasswordUtil;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.presentation.ShoppingCall;
import br.com.wdc.shopping.presentation.presenter.open.login.structs.Subject;

/** Autentica o usuário; {@code null} se as credenciais forem inválidas. */
public class LoginCall extends ShoppingCall<LoginCall.Result> {

    /**
     * Resultado de uma autenticação bem-sucedida.
     *
     * @param subject         dados do usuário autenticado
     * @param persistentToken token de auto-login (remember me); {@code null} em clientes REST
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

        // 5. Token persistente para auto-login (remember me)
        // REST clients gerenciam sessão via refresh token; lança UnsupportedOperationException.
        String persistentToken = null;
        try {
            persistentToken = authService.createPersistentToken(authResult.userId(), userName);
        } catch (UnsupportedOperationException ignored) {
            // REST client: sem persistent token
        }

        // 6. Buscar nome de exibição do usuário
        var users = userRepository().fetch(new UserCriteria()
                .withUserId(authResult.userId())
                .withProjection(Subject.projection()), 0, 1);
        var subject = users.isEmpty() ? null : Subject.create(users.get(0));

        return new Result(subject, persistentToken);
    }

    /**
     * Fallback para ambientes sem segurança (testes unitários sem initializeSecurity).
     *
     * <p>
     * Busca pelo login e <b>compara o resumo aqui</b>, em vez de mandá-lo como critério. A senha não é campo por onde
     * se pesquisa: deixá-la na superfície do critério a exporia também na API REST, onde filtrar usuário por senha não
     * serve a ninguém. É o mesmo caminho que o {@code AuthenticationServiceImpl} segue no fluxo real — projeta o
     * resumo e o confere — só que sem o desafio, que aqui não existe.
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
        // A coluna é CHAR e vem preenchida com espaços à direita; sem o corte a comparação nunca casaria.
        var stored = user.password() == null ? null : user.password().trim();
        if (stored == null || !stored.equals(PasswordUtil.hashPassword(password))) {
            return null;
        }

        return new Result(Subject.create(user), null);
    }

}
