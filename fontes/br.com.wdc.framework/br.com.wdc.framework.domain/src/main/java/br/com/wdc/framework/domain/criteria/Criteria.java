package br.com.wdc.framework.domain.criteria;

import java.util.List;

/**
 * Contract shared by every {@code XxxCriteria}: exposes the filterable fields as a list.
 *
 * <p>
 * It exists so that translating the criteria into a condition is written <b>once</b>. Without it, each entity needs one
 * {@code if} per field, and every new operator means rewriting that block in every entity. With the list, the core
 * walks the provided fields and all that is left for each entity is to say which column matches each name.
 * </p>
 *
 * <p>
 * The order is the declaration order in the criteria, and it is the order in which the conditions enter the
 * {@code AND} — hence predictable in the generated SQL.
 * </p>
 */
public interface Criteria {

    /**
     * The fields that were actually instantiated.
     *
     * <p>
     * A field is only created when someone asks for it, so this list holds the touched ones — set or not. Telling
     * "touched" from "filters" is done with {@link Criterion#isSet()}, and is the translator's responsibility.
     * </p>
     */
    List<Criterion<?, ?>> criterions();

    /**
     * The alternative groups: the criteria matches when its own fields match <b>and</b> at least one group matches.
     *
     * <p>
     * The fields of a criteria always join with {@code AND}, and {@code or()} only joins the requests of a single field.
     * A disjunction across fields — "my purchases, or the ones of my store" — needs groups: each one is a criteria of
     * the same entity, with its fields in {@code AND}, and the groups join with {@code OR}. A group may have groups of
     * its own. An empty list means no alternative; a group with no field set matches every row.
     * </p>
     *
     * <p>
     * Only the filter fields of a group count: its projection and its ordering are ignored, since the query has one of
     * each, and they belong to the outer criteria.
     * </p>
     *
     * <p>
     * <b>The groups are for code on the server</b> — the data policy uses them to restrict the rows a role reaches —
     * and do not travel: the codecs refuse them on both sides of the wire ({@code CriterionCodec.refuseAnyOf}). A
     * disjunction chosen by the client would be any tree of {@code OR}s over any columns; what a client needs, a term
     * searched in several columns, is a criteria field of its own, translated by the repository.
     * </p>
     */
    List<? extends Criteria> anyOf();
}
