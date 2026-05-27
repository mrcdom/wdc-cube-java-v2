package br.com.wdc.shopping.persistence.repository.user;

import java.util.List;

import br.com.wdc.framework.commons.util.TransactionContext;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.Page;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.persistence.repository.BaseRepository;

public class UserRepositoryImpl extends BaseRepository implements UserRepository {

    @Override
    public boolean insert(User user) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return InsertRowUserCmd.run(tx.connection(), user);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public boolean update(User newUser, User oldUser) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return UpdateRowUserCmd.run(tx.connection(), newUser, oldUser);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public int delete(UserCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return DeleteUsersCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public int count(UserCriteria criteria) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return CountUsersCmd.byCriteria(tx.connection(), criteria);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public List<User> fetch(UserCriteria criteria, int offset, int limit) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchUsersCmd.byCriteria(tx.connection(), criteria, offset, limit);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

    @Override
    public Page<User> fetchPage(UserCriteria criteria, int page, int pageSize) {
        var total = count(criteria);
        var items = this.fetch(criteria, page * pageSize, pageSize);
        return Page.of(items, page, pageSize, total);
    }

    @Override
    public User fetchById(Long userId, User projection) {
        try (var tx = TransactionContext.begin(dataSource())) {
            return FetchUsersCmd.byId(tx.connection(), userId, projection);
        } catch (Exception caught) {
            throw readException(caught);
        }
    }

}
