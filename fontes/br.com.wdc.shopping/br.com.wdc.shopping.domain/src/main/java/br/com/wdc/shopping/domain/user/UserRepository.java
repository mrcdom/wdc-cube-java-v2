package br.com.wdc.shopping.domain.user;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;

public interface UserRepository extends Repository<User, UserCriteria, Long> {

    AtomicReference<UserRepository> BEAN = new AtomicReference<>();
    
    @Override
    default User newProjection() {
        var pv = ProjectionValues.INSTANCE;

        User prj = new User()
                .withId(pv.i64)
                .withUserName(pv.str)
                .withPassword(pv.str)
                .withName(pv.str)
                .withRoles(pv.str);
        return prj;
    }

}
