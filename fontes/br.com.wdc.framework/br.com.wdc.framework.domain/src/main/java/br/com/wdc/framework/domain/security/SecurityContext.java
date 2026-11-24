package br.com.wdc.framework.domain.security;

import java.security.PrivateKey;
import java.util.Set;

/**
 * Security context of an authenticated session.
 * <p>
 * Available via {@code SecurityContext.CURRENT} (ThreadLocal) to any
 * layer that needs to check permissions or the identity of the current user.
 */
public interface SecurityContext {

	ThreadLocal<SecurityContext> CURRENT = new ThreadLocal<>();

	Long userId();

	String userName();

	/** The names of the user's roles — what a {@code DataPolicy} grants by. */
	Set<String> roles();

	Set<String> permissions();

	boolean hasPermission(String entity, String operation);

	boolean hasDataAll();

	PrivateKey privateKey();

	String publicKeyBase64();

}
