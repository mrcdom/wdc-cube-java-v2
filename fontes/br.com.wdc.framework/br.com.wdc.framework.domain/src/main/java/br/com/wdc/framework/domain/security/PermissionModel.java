package br.com.wdc.framework.domain.security;

import java.util.Set;

/**
 * Generic evaluation of permissions in the {@code entity:operation} format.
 * <p>
 * Rules:
 * <ul>
 * <li>A permission {@code entity:operation} grants that operation on the entity.</li>
 * <li>The wildcard {@code entity:*} grants every operation on the entity.</li>
 * <li>{@link #DATA_ALL} grants unrestricted access to the data of all users.</li>
 * </ul>
 * The model is <em>allow-wins</em>: the effective permission set is the union of the
 * permissions of all the user's roles; the application defines which roles and
 * permissions exist.
 */
public final class PermissionModel {

    /** Special permission that grants unrestricted access to the data of all users. */
    public static final String DATA_ALL = "data:all";

    private PermissionModel() {
    }

    /**
     * Checks whether the permission set grants the operation on the entity
     * (taking the wildcard {@code entity:*} into account).
     */
    public static boolean grants(Set<String> permissions, String entity, String operation) {
        return permissions.contains(entity + ":" + operation)
                || permissions.contains(entity + ":*");
    }

    /**
     * Checks whether the permissions include unrestricted access to the data of all users.
     */
    public static boolean hasDataAll(Set<String> permissions) {
        return permissions.contains(DATA_ALL);
    }
}
