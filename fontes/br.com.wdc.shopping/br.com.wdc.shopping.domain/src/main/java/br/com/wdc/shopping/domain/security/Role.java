package br.com.wdc.shopping.domain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Papéis de acesso com permissões definidas em código.
 * <p>
 * Permissões seguem o formato {@code entidade:operação}.
 * Wildcard {@code *} concede todas as operações na entidade.
 * <p>
 * Modelo allow-wins: a permissão efetiva é a UNIÃO de todos os papéis do usuário.
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
	 * Calcula as permissões efetivas (allow-wins) para um conjunto de papéis.
	 */
	public static Set<String> effectivePermissions(Set<Role> roles) {
		return roles.stream()
				.flatMap(r -> r.permissions.stream())
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Parseia uma string de papéis separados por vírgula (ex: "ADMIN,CUSTOMER").
	 * Papéis desconhecidos são ignorados silenciosamente.
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
	 * Verifica se o conjunto de permissões concede acesso à operação na entidade.
	 */
	public static boolean hasPermission(Set<String> permissions, String entity, String operation) {
		return permissions.contains(entity + ":" + operation)
				|| permissions.contains(entity + ":*");
	}

	/**
	 * Verifica se as permissões incluem acesso irrestrito a dados de todos os usuários.
	 */
	public static boolean hasDataAll(Set<String> permissions) {
		return permissions.contains("data:all");
	}
}
