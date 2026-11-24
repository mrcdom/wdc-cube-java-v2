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
}
