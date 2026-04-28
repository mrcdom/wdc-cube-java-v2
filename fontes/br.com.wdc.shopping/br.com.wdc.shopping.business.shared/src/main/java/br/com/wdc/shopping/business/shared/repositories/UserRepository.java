package br.com.wdc.shopping.business.shared.repositories;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.business.shared.criteria.UserCriteria;
import br.com.wdc.shopping.business.shared.model.User;

public interface UserRepository {

    AtomicReference<UserRepository> BEAN = new AtomicReference<>();

    boolean insert(User user);

    boolean update(User newUser, User oldUser);

    boolean insertOrUpdate(User user);

    int delete(UserCriteria criteria);

    int count(UserCriteria criteria);

    List<User> fetch(UserCriteria criteria);

    User fetchById(Long userId, User projection);

}
