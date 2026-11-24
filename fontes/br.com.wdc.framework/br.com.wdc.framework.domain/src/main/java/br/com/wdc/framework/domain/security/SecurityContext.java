package br.com.wdc.framework.domain.security;

import java.security.PrivateKey;
import java.util.Set;

/**
 * Security context of an authenticated session.
 * <p>
 * Available via {@code SecurityContext.CURRENT} (ThreadLocal) to any
 * layer that needs the identity or the roles of the current user.
 */
public interface SecurityContext {

	ThreadLocal<SecurityContext> CURRENT = new ThreadLocal<>();

	Long userId();

	String userName();

	/** The names of the user's roles — what a {@code DataPolicy} grants by. */
	Set<String> roles();

	PrivateKey privateKey();

	String publicKeyBase64();

}
