package br.com.wdc.shopping.persistence.impl.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.shopping.persistence.impl.scheme.Sequences.SQ_USER;
import static br.com.wdc.shopping.persistence.impl.scheme.tables.EnUser.EN_USER;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.jooq.SortField;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.domain.user.UserCriteria;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.persistence.impl.scheme.tables.EnUser;
import br.com.wdc.shopping.persistence.impl.util.BaseRepositoryImpl;

public class UserRepositoryImpl extends BaseRepositoryImpl  implements UserRepository {
    
    // @formatter:off
    public static final JsonQuery<User, EnUser> QUERY = new JsonQueryBuilder<User, EnUser>()
            .setAlias("u")
            .setBeanFactory(User::new)
            .setTableFactory(EN_USER::as)
            .setDSLContextSupplier(UserRepositoryImpl::dsl)
            .setOrdering(UserRepositoryImpl::orderingOf)
            .addI64("id", u -> u.id(), (u, v) -> u.withId(v), t -> t.ID)
            .addStr("userName", u -> u.userName(), (u, v) -> u.withUserName(v), t -> t.USERNAME)
            .addStr("password", u -> u.password(), (u, v) -> u.withPassword(v), t -> t.PASSWORD)
            .addStr("name", u -> u.name(), (u, v) -> u.withName(v), t -> t.NAME)
            .addStr("roles", u -> u.roles(), (u, v) -> u.withRoles(v), t -> t.ROLES)
            .build();
    // @formatter:on

    /**
     * Traduz o {@code OrderBy} do critério em {@code ORDER BY}, contra a tabela informada.
     *
     * <p>
     * Recebe {@code Object} porque também é chamado a partir da coleção filha de outro repositório, onde o critério
     * pode não ser deste tipo — nesse caso não ordena nada. A tabela vem por parâmetro porque, dentro de um subselect
     * correlacionado, a instância tem alias próprio.
     * </p>
     */
    public static List<SortField<?>> orderingOf(EnUser t, Object criteriaObj) {
        if (!(criteriaObj instanceof UserCriteria criteria) || criteria.orderBy() == null) {
            return List.of();
        }
        return switch (criteria.orderBy()) {
        case ASCENDING -> List.of(t.ID.asc());
        case DESCENDING -> List.of(t.ID.desc());
        };
    }

    // :: Query helpers

    public static Condition applyConditions(JsonChildQueryBuilder<?, EnUser> cq) {
        if (cq.getCriteria() instanceof UserCriteria criteria) {
            return new ApplyConditions(cq.getChildTable(), cq.getCtx()).apply(criteria);
        }
        return DSL.noCondition();
    }

    private static Condition applyConditions(EnUser t, UserCriteria criteria) {
        return new ApplyConditions(t).apply(criteria);
    }

    // :: UserRepository implementation

    @Override
    public boolean insert(User user) {
        var dsl = dsl();

        if (user.id() == null) {
            user.withId(dsl.nextval(SQ_USER));
        }

        var step = dsl.insertInto(EN_USER)
                .set(EN_USER.ID, user.id());

        if (user.userName() != null) {
            step.set(EN_USER.USERNAME, user.userName());
        }
        if (user.password() != null) {
            step.set(EN_USER.PASSWORD, user.password());
        }
        if (user.name() != null) {
            step.set(EN_USER.NAME, user.name());
        }
        if (user.roles() != null) {
            step.set(EN_USER.ROLES, user.roles());
        }

        return step.execute() > 0;
    }

    @Override
    public boolean update(User newBean, User oldBean, User projection) {
        if (newBean == null) {
            throw new AssertionError("newBean is requeried");
        }

        if (newBean.id() == null) {
            throw new AssertionError("Missing primary key");
        }

        if (projection == null) {
            projection = this.newProjection();
        }

        var dsl = dsl();
        var step = dsl.update(EN_USER).set(EN_USER.ID, newBean.id());

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, u -> u.userName())) {
            step.set(EN_USER.USERNAME, newBean.userName());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.password())) {
            step.set(EN_USER.PASSWORD, newBean.password());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.name())) {
            step.set(EN_USER.NAME, newBean.name());
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.roles())) {
            step.set(EN_USER.ROLES, newBean.roles());
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_USER.ID.eq(newBean.id())).execute() > 0;
    }

    @Override
    public int delete(UserCriteria criteria) {
        if (criteria == null || criteria.userId() == null) {
            throw new AssertionError("Missing primary key");
        }

        var dsl = dsl();
        return dsl.deleteFrom(EN_USER)
                .where(applyConditions(EN_USER, criteria))
                .execute();
    }

    @Override
    public int count(UserCriteria criteria) {
        var dsl = dsl();
        return dsl.selectCount()
                .from(EN_USER)
                .where(applyConditions(EN_USER, criteria))
                .fetchOne()
                .value1();
    }

    @Override
    public List<User> fetch(UserCriteria criteria, int offset, int limit) {
        var prjBean = projectionFrom(criteria);

        return QUERY.fetchToList(prjBean, (t, q) -> {
            var cond = applyConditions(t, criteria);
            var step = q.where(cond);

            step.orderBy(orderingOf(t, criteria));

            if (limit > 0) {
                step.limit(limit);
            }
            if (offset > 0) {
                step.offset(offset);
            }
        });
    }

    @Override
    public User fetchById(Long userId, User projection) {
        var prjBean = projection != null ? projection : QUERY.newProjectionBean();
        if (prjBean.id() == null) {
            prjBean.withId(0L);
        }

        return QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(userId)));
    }

    // :: Internal

    private User projectionFrom(UserCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            if (prj.id() == null) {
                prj.withId(0L);
            }
            return prj;
        }
        return this.newProjection();
    }

    static class ApplyConditions {

        EnUser enUser;
        QueryContext ctx;

        ApplyConditions(EnUser t, QueryContext ctx) {
            this.enUser = t;
            this.ctx = ctx;
        }

        ApplyConditions(EnUser t) {
            this.enUser = t;
            this.ctx = new QueryContext(dsl());
        }

        public Condition apply(UserCriteria criteria) {
            var condition = DSL.noCondition();
            if (criteria == null) {
                return condition;
            }
            if (criteria.userId() != null) {
                condition = condition.and(enUser.ID.eq(criteria.userId()));
            }
            if (criteria.userName() != null) {
                condition = condition.and(enUser.USERNAME.eq(criteria.userName()));
            }
            if (criteria.password() != null) {
                var hashedPassword = new BigInteger(md5().digest(
                        criteria.password().getBytes(StandardCharsets.UTF_8))).toString(36);
                condition = condition.and(enUser.PASSWORD.eq(hashedPassword));
            }
            return condition;
        }

        private static MessageDigest md5() {
            try {
                return MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        }
    }
}
