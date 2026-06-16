package br.com.wdc.shopping.persistence.rest.security;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * Utilitário de verificação de segurança para os endpoints REST.
 * <p>
 * Verifica que há um {@link SecurityContext} na thread corrente e que possui a permissão solicitada.
 * <p>
 * Se o {@link AuthenticationService} não estiver configurado (modo teste/local), a verificação é ignorada e retorna
 * {@code null}.
 */
public final class SecurityEnforcer {

    private SecurityEnforcer() {
    }

    /**
     * Exige que haja um SecurityContext e que tenha a permissão solicitada.
     * <p>
     * Se o {@link AuthenticationService} não estiver configurado, retorna {@code null} (modo sem segurança — testes ou
     * acesso local).
     *
     * @return o SecurityContext corrente, ou {@code null} se segurança não está ativa
     * @throws AccessDeniedException se segurança está ativa e o usuário não está autenticado ou sem permissão
     */
    public static SecurityContext require(String entity, String operation) {
        if (AuthenticationService.BEAN.get() == null) {
            return null;
        }
        var sc = SecurityContext.CURRENT.get();
        if (sc == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!sc.hasPermission(entity, operation)) {
            throw new AccessDeniedException("Requires " + entity + ":" + operation);
        }
        return sc;
    }

}
