package br.com.wdc.framework.commons.serialization;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Rastreia entidades já processadas em uma operação de serialização/deserialização, usando
 * <b>identidade de instância</b> (referência Java {@code ==}) para detectar referências cíclicas.
 * <p>
 * Duas instâncias distintas com a mesma chave ({@link KeyedEntity#key()}) são consideradas independentes —
 * somente a mesma instância é detectada como duplicata. Quando detectada, o codec deve usar um stub
 * (nova instância com apenas a chave preenchida, obtida via {@link KeyedEntity#key()}).
 * <p>
 * Escopo: uma instância por operação top-level (um {@code writeEntity} ou {@code readEntity} raiz).
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
     * Rastreia a entidade por identidade de instância. Retorna {@code true} se é a primeira ocorrência,
     * {@code false} se esta mesma instância já foi rastreada (ciclo — usar stub com {@link KeyedEntity#key()}).
     * <p>
     * {@code null} sempre retorna {@code true} (não rastreável).
     */
    public boolean track(KeyedEntity entity) {
        if (entity == null) return true;
        return seen.add(entity);
    }

    /**
     * Verifica se esta mesma instância já foi rastreada.
     */
    public boolean isSeen(KeyedEntity entity) {
        if (entity == null) return false;
        return seen.contains(entity);
    }
}
