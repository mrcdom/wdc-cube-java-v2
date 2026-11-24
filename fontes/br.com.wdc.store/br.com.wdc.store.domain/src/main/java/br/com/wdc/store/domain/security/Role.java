package br.com.wdc.store.domain.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The access roles of the store.
 * <p>
 * A role carries no permissions of its own: what each one may do with the data, through the REST API, is declared by
 * the data access policies of the REST layer ({@code UserDataPolicy}, {@code PurchaseDataPolicy}, ...), by the role
 * name. A user may have several roles; the model is <em>allow-wins</em>.
 */
public enum Role {

	ADMIN,

	CUSTOMER,

	MANAGER;

	/**
	 * The names of the roles, as a {@code SecurityContext} carries them.
	 */
	public static Set<String> names(Set<Role> roles) {
		return roles.stream()
				.map(Role::name)
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
}
