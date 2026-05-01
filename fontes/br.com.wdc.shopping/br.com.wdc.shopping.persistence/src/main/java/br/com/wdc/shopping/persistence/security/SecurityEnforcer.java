package br.com.wdc.shopping.persistence.security;

import br.com.wdc.shopping.domain.exception.AccessDeniedException;
import br.com.wdc.shopping.domain.security.SecurityContext;
import br.com.wdc.shopping.domain.security.SecurityContextHolder;

/**
 * Utilitários de verificação de segurança para os decorators de repositório.
 */
final class SecurityEnforcer {

	private SecurityEnforcer() {
	}

	/**
	 * Exige que haja um SecurityContext e que tenha a permissão solicitada.
	 *
	 * @return o SecurityContext corrente (nunca null)
	 * @throws AccessDeniedException se não autenticado ou sem permissão
	 */
	static SecurityContext require(String entity, String operation) {
		var sc = SecurityContextHolder.get();
		if (sc == null) {
			throw new AccessDeniedException("Authentication required");
		}
		if (!sc.hasPermission(entity, operation)) {
			throw new AccessDeniedException("Requires " + entity + ":" + operation);
		}
		return sc;
	}

}
