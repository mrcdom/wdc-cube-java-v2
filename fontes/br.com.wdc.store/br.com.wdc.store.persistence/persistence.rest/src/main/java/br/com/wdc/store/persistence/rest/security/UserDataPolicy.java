package br.com.wdc.store.persistence.rest.security;

import java.util.Set;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.DataPolicy;
import br.com.wdc.framework.domain.security.policy.EntityDescriptor;
import br.com.wdc.framework.domain.security.policy.Mask;
import br.com.wdc.framework.domain.security.policy.RowScope;
import br.com.wdc.store.domain.security.Role;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;

/**
 * Who may do what with the users through the REST API.
 *
 * <ul>
 * <li><b>ADMIN</b> reads every user, creates, updates and deletes them.</li>
 * <li><b>CUSTOMER</b> reads its own user.</li>
 * <li><b>MANAGER</b> reads its own user in full, and the others by name, with the login masked.</li>
 * </ul>
 *
 * <p>
 * The password is in no grant: it is written when a user is created or updated, and never read back.
 * </p>
 */
public final class UserDataPolicy {

    private static final ProjectionValues PV = ProjectionValues.INSTANCE;

    private UserDataPolicy() {
    }

    // @formatter:off
    public static final EntityDescriptor<User, UserCriteria> USER = EntityDescriptor.<User, UserCriteria>builder("user")
            .entity(User::new)
            .criteria(UserCriteria::new)
            .projection(UserCriteria::projection, UserCriteria::withProjection)
            .anyOf(UserCriteria::anyOf, UserCriteria::withAnyOf)
            .key("id", User::id, User::withId, PV.i64)
            .field("userName", User::userName, User::withUserName, PV.str)
            .field("password", User::password, User::withPassword, PV.str)
            .field("name", User::name, User::withName, PV.str)
            .field("roles", User::roles, User::withRoles, PV.str)
            .criterion("userId", "id")
            .orderings(UserDataPolicy::orderingFields)
            .build();
    // @formatter:on

    /** The requester's own user. */
    public static final RowScope<User, UserCriteria> OWN = new RowScope<>() {

        @Override
        public void restrict(UserCriteria group, SecurityContext sc) {
            group.withUserId(sc.userId());
        }

        @Override
        public boolean covers(User row, SecurityContext sc) {
            return sc.userId().equals(row.id());
        }

        @Override
        public Set<String> fields() {
            return Set.of();
        }

        @Override
        public String toString() {
            return "OWN";
        }
    };

    public static final DataPolicy<User, UserCriteria> POLICY = DataPolicy.on(USER)
            .grant(Role.ADMIN.name(), g -> g
                    .read(new User()
                            .withUserName(PV.str)
                            .withName(PV.str)
                            .withRoles(PV.str))
                    .create(new User()
                            .withUserName(PV.str)
                            .withPassword(PV.str)
                            .withName(PV.str)
                            .withRoles(PV.str))
                    .update(new User()
                            .withUserName(PV.str)
                            .withPassword(PV.str)
                            .withName(PV.str)
                            .withRoles(PV.str))
                    .delete())
            .grant(Role.CUSTOMER.name(), g -> g
                    .rows(OWN)
                    .read(ownUser()))
            .grant(Role.MANAGER.name(), g -> g
                    .rows(OWN)
                    .read(ownUser()))
            .grant(Role.MANAGER.name(), g -> g
                    .read(new User()
                            .withName(PV.str))
                    .mask(new User()
                            .withUserName(PV.str), Mask.keepFirst(1)))
            .build();

    /** What a user reads of their own record: the signed-in user is shown by the client. */
    private static User ownUser() {
        return new User()
                .withUserName(PV.str)
                .withName(PV.str)
                .withRoles(PV.str);
    }

    /** The fields each provisioned ordering sorts by; the tie-breaker is the key, which is not checked. */
    private static Set<String> orderingFields(UserCriteria criteria) {
        if (criteria.orderBy() == null) {
            return Set.of();
        }
        return switch (criteria.orderBy()) {
        case OLDEST_FIRST, NEWEST_FIRST -> Set.of();
        case NAME_A_TO_Z -> Set.of("name");
        case LOGIN_A_TO_Z -> Set.of("userName");
        };
    }
}
