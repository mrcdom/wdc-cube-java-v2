package br.com.wdc.shopping.domain.user;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;

public interface UserRepository extends Repository<User, UserCriteria, Long> {

    AtomicReference<UserRepository> BEAN = new AtomicReference<>();
    
    @Override
    default User newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new User()
                .withId(pv.i64)
                .withUserName(pv.str)
                .withPassword(pv.str)
                .withName(pv.str)
                .withRoles(pv.str);
    }

    /**
     * Lookup by key: a {@code UserCriteria} with equality on the primary key, resolved by {@code fetch}.
     *
     * @param projection {@code null} projects {@link #newProjection()} — all the entity's shallow fields.
     * @return the entity, or {@code null} if there is no row with that key.
     */
    @Override
    default User fetchById(Long userId, User projection) {
        if (userId == null) {
            return null;
        }

        var found = fetch(new UserCriteria()
                .withUserId(userId)
                .withProjection(projection != null ? projection : newProjection()), 0, 1);

        return found.isEmpty() ? null : found.get(0);
    }

}
