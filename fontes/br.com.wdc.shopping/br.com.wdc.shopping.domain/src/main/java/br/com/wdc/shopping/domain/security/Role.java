package br.com.wdc.shopping.domain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.wdc.framework.domain.security.PermissionModel;

/**
 * Access roles with permissions defined in code.
 * <p>
 * Permissions follow the {@code entity:operation} format.
 * The {@code *} wildcard grants all operations on the entity.
 * <p>
 * Allow-wins model: the effective permission is the UNION of all the user's roles.
 */
public enum Role {

	ADMIN(Set.of(
			"user:*",
			"product:*",
			"purchase:*",
			"purchase-item:*",
			"data:all")),

	CUSTOMER(Set.of(
			"product:read",
			"purchase:read", "purchase:write",
			"purchase-item:read", "purchase-item:write")),

	MANAGER(Set.of(
			"product:read", "product:write",
			"purchase:read",
			"purchase-item:read"));

	private final Set<String> permissions;

	Role(Set<String> permissions) {
		this.permissions = Collections.unmodifiableSet(permissions);
	}

	public Set<String> permissions() {
		return permissions;
	}

	/**
	 * Computes the effective permissions (allow-wins) for a set of roles.
	 */
	public static Set<String> effectivePermissions(Set<Role> roles) {
		return roles.stream()
				.flatMap(r -> r.permissions.stream())
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Parses a string of comma-separated roles (e.g. "ADMIN,CUSTOMER").
	 * Unknown roles are silently ignored.
	 */
	public static Set<Role> parse(String rolesStr) {
		if (rolesStr == null || rolesStr.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(rolesStr.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.flatMap(s -> {
					try {
						return java.util.stream.Stream.of(Role.valueOf(s));
					} catch (IllegalArgumentException e) {
						return java.util.stream.Stream.empty();
					}
				})
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Checks whether the set of permissions grants access to the operation on the entity.
	 */
	public static boolean hasPermission(Set<String> permissions, String entity, String operation) {
		return PermissionModel.grants(permissions, entity, operation);
	}

	/**
	 * Checks whether the permissions include unrestricted access to all users' data.
	 */
	public static boolean hasDataAll(Set<String> permissions) {
		return PermissionModel.hasDataAll(permissions);
	}
}
