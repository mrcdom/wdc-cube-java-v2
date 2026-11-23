package br.com.wdc.framework.commons.serialization;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Tracks entities already processed in a serialization/deserialization operation, using
 * <b>instance identity</b> (Java reference {@code ==}) to detect cyclic references.
 * <p>
 * Two distinct instances with the same key ({@link KeyedEntity#key()}) are considered independent —
 * only the same instance is detected as a duplicate. When detected, the codec must use a stub
 * (a new instance with only the key filled in, obtained via {@link KeyedEntity#key()}).
 * <p>
 * Scope: one instance per top-level operation (one root {@code writeEntity} or {@code readEntity}).
 *
 * <pre>{@code
 * var graph = new EntityGraph();
 * graph.track(purchase);        // first time → true
 * graph.track(purchase);        // same instance → false (already seen)
 * graph.isSeen(purchase);       // true
 *
 * var other = new Purchase();
 * other.id = purchase.id;
 * graph.track(other);           // different instance, same key → true (allowed)
 * }</pre>
 */
public class EntityGraph {

    private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Tracks the entity by instance identity. Returns {@code true} if this is the first occurrence,
     * {@code false} if this same instance was already tracked (cycle — use a stub with {@link KeyedEntity#key()}).
     * <p>
     * {@code null} always returns {@code true} (not trackable).
     */
    public boolean track(KeyedEntity entity) {
        if (entity == null) return true;
        return seen.add(entity);
    }

    /**
     * Checks whether this same instance was already tracked.
     */
    public boolean isSeen(KeyedEntity entity) {
        if (entity == null) return false;
        return seen.contains(entity);
    }
}
