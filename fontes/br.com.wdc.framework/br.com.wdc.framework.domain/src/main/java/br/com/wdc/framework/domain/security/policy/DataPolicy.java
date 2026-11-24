package br.com.wdc.framework.domain.security.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * Who may do what with the data of an entity through an API open to external clients: per role, the rows it reaches,
 * the fields it reads (as they are or masked), the fields it writes, and whether it deletes.
 *
 * <p>
 * It exists for the boundary where the request is assembled by the client — the REST repository API, which accepts any
 * criteria and any projection. Code that runs on the server (a presenter, a resource call) chooses its own queries and
 * uses the repositories directly, without a policy.
 * </p>
 *
 * <p>
 * The model is <em>allow-wins</em>: a user with several roles gets every grant of every role, and a request is allowed
 * when some grant allows it. Fields are decided <b>per row</b>: a row shows what the grants that reach it allow, so
 * reading everyone's name in one role and one's own document in another does not add up to everyone's document.
 * </p>
 *
 * <pre>
 * public static final DataPolicy&lt;User, UserCriteria&gt; POLICY = DataPolicy.on(USER)
 *         .grant("ADMIN", g -&gt; g
 *                 .read(...)
 *                 .update(...)
 *                 .delete())
 *         .grant("CUSTOMER", g -&gt; g
 *                 .rows(OWN)
 *                 .read(...))
 *         .build();
 * </pre>
 *
 * @param <E> the entity
 * @param <C> its criteria
 */
public final class DataPolicy<E, C extends Criteria> {

    private final EntityDescriptor<E, C> entity;
    private final Map<String, List<DataGrant<E, C>>> grantsByRole;

    private DataPolicy(Builder<E, C> b) {
        this.entity = b.entity;
        var copy = new LinkedHashMap<String, List<DataGrant<E, C>>>();
        b.grantsByRole.forEach((role, grants) -> copy.put(role, List.copyOf(grants)));
        this.grantsByRole = Collections.unmodifiableMap(copy);
    }

    public static <E, C extends Criteria> Builder<E, C> on(EntityDescriptor<E, C> entity) {
        return new Builder<>(entity);
    }

    public EntityDescriptor<E, C> entity() {
        return entity;
    }

    /** The grants of a role; empty when the role has none on this entity. */
    public List<DataGrant<E, C>> grantsOf(String role) {
        return grantsByRole.getOrDefault(role, List.of());
    }

    /**
     * What the requester may do. Without a security context — security is off, in local or test mode — the access is
     * unrestricted.
     */
    public DataAccess<E, C> accessFor(SecurityContext sc) {
        if (sc == null) {
            return DataAccess.unrestricted(entity);
        }
        var grants = new ArrayList<DataGrant<E, C>>();
        for (var role : sc.roles()) {
            grants.addAll(grantsOf(role));
        }
        return new DataAccess<>(entity, sc, grants);
    }

    public static final class Builder<E, C extends Criteria> {

        private final EntityDescriptor<E, C> entity;
        private final Map<String, List<DataGrant<E, C>>> grantsByRole = new LinkedHashMap<>();

        private Builder(EntityDescriptor<E, C> entity) {
            this.entity = entity;
        }

        /** Adds a grant to the role; a role may have several, one per set of rows. */
        public Builder<E, C> grant(String role, Consumer<DataGrant.Builder<E, C>> declaration) {
            var builder = DataGrant.builder(entity);
            declaration.accept(builder);
            grantsByRole.computeIfAbsent(role, r -> new ArrayList<>()).add(builder.build());
            return this;
        }

        public DataPolicy<E, C> build() {
            return new DataPolicy<>(this);
        }
    }
}
