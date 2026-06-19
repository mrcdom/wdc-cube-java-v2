package br.com.wdc.shopping.domain.repositories;

import br.com.wdc.framework.domain.repository.Repository;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public interface UserRepository extends Repository<User, UserCriteria, Long> {

    AtomicReference<UserRepository> BEAN = new AtomicReference<>();
    
    @Override
    default User newProjection() {
        var pv = ProjectionValues.INSTANCE;

        User prj = new User();
        prj.id = pv.i64;
        prj.userName = pv.str;
        prj.password = pv.str;
        prj.name = pv.str;
        prj.roles = pv.str;
        return prj;
    }

}
