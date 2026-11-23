package br.com.wdc.framework.commons.serialization;

/**
 * Contract for entities that have an identity key.
 * <p>
 * Lets serialization mechanisms detect repeated references in the object graph
 * and replace them with stubs holding only the key, avoiding infinite recursion.
 */
public interface KeyedEntity {

    /**
     * Returns the entity's identity key (typically the ID).
     * May be {@code null} for entities not yet persisted.
     */
    Object key();
}
