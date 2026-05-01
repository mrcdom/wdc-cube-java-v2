package br.com.wdc.shopping.domain.security;

import java.security.PrivateKey;
import java.util.Set;

/**
 * Contexto de segurança de uma sessão autenticada.
 * <p>
 * Disponível via {@link SecurityContextHolder} para qualquer camada
 * que precise verificar permissões ou identidade do usuário corrente.
 */
public interface SecurityContext {

	Long userId();

	String userName();

	Set<String> permissions();

	boolean hasPermission(String entity, String operation);

	boolean hasDataAll();

	PrivateKey privateKey();

	String publicKeyBase64();

}
