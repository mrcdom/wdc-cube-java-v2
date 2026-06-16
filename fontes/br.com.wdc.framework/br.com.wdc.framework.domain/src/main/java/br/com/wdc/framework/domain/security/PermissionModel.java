package br.com.wdc.framework.domain.security;

import java.util.Set;

/**
 * Avaliação genérica de permissões no formato {@code entidade:operação}.
 * <p>
 * Regras:
 * <ul>
 * <li>Uma permissão {@code entidade:operação} concede aquela operação na entidade.</li>
 * <li>O wildcard {@code entidade:*} concede todas as operações na entidade.</li>
 * <li>{@link #DATA_ALL} concede acesso irrestrito a dados de todos os usuários.</li>
 * </ul>
 * O modelo é <em>allow-wins</em>: o conjunto efetivo de permissões é a união das
 * permissões de todos os papéis do usuário; a aplicação define quais papéis e
 * permissões existem.
 */
public final class PermissionModel {

    /** Permissão especial que concede acesso irrestrito a dados de todos os usuários. */
    public static final String DATA_ALL = "data:all";

    private PermissionModel() {
    }

    /**
     * Verifica se o conjunto de permissões concede a operação na entidade
     * (considerando o wildcard {@code entidade:*}).
     */
    public static boolean grants(Set<String> permissions, String entity, String operation) {
        return permissions.contains(entity + ":" + operation)
                || permissions.contains(entity + ":*");
    }

    /**
     * Verifica se as permissões incluem acesso irrestrito a dados de todos os usuários.
     */
    public static boolean hasDataAll(Set<String> permissions) {
        return permissions.contains(DATA_ALL);
    }
}
