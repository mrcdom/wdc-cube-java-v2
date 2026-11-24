package br.com.wdc.store.persistence.impl.security;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

import br.com.wdc.framework.domain.security.PermissionModel;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * {@link SecurityContext} implementation for an authenticated session.
 * Holds the userId, the roles, the effective permissions and the ephemeral RSA key pair.
 */
public final class AccessContext implements SecurityContext {

	private final Long userId;
	private final String userName;
	private final Set<String> roles;
	private final Set<String> permissions;
	private final KeyPair rsaKeyPair;
	private final Instant expiresAt;
	private final String refreshToken;

	public AccessContext(Long userId, String userName, Set<String> roles, Set<String> permissions,
			KeyPair rsaKeyPair, Instant expiresAt, String refreshToken) {
		this.userId = userId;
		this.userName = userName;
		this.roles = roles;
		this.permissions = permissions;
		this.rsaKeyPair = rsaKeyPair;
		this.expiresAt = expiresAt;
		this.refreshToken = refreshToken;
	}

	@Override
	public Long userId() {
		return userId;
	}

	@Override
	public String userName() {
		return userName;
	}

	@Override
	public Set<String> roles() {
		return roles;
	}

	@Override
	public Set<String> permissions() {
		return permissions;
	}

	@Override
	public PrivateKey privateKey() {
		return rsaKeyPair.getPrivate();
	}

	public PublicKey publicKey() {
		return rsaKeyPair.getPublic();
	}

	@Override
	public String publicKeyBase64() {
		return Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public String refreshToken() {
		return refreshToken;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}

	@Override
	public boolean hasPermission(String entity, String operation) {
		return PermissionModel.grants(permissions, entity, operation);
	}

	@Override
	public boolean hasDataAll() {
		return PermissionModel.hasDataAll(permissions);
	}
}
