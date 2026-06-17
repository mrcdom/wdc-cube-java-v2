package br.com.wdc.shopping.persistence.impl.repository;

import static br.com.wdc.framework.domain.repository.Repository.changed;
import static br.com.wdc.shopping.persistence.impl.jooq.Sequences.SQ_USER;
import static br.com.wdc.shopping.persistence.impl.jooq.tables.EnUser.EN_USER;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.jooq.Condition;
import org.jooq.impl.DSL;

import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.framework.jooq.JsonChildQueryBuilder;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.framework.jooq.QueryContext;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.persistence.impl.jooq.tables.EnUser;

public class UserRepositoryImpl implements UserRepository {

    // @formatter:off
    public static final JsonQuery<User, EnUser> QUERY = new JsonQueryBuilder<User, EnUser>()
            .setAlias("u")
            .setBeanFactory(User::new)
            .setTableFactory(EN_USER::as)
            .addI64("id", u -> u.id, (u, v) -> u.id = v, t -> t.ID)
            .addStr("userName", u -> u.userName, (u, v) -> u.userName = v, t -> t.USERNAME)
            .addStr("password", u -> u.password, (u, v) -> u.password = v, t -> t.PASSWORD)
            .addStr("name", u -> u.name, (u, v) -> u.name = v, t -> t.NAME)
            .addStr("roles", u -> u.roles, (u, v) -> u.roles = v, t -> t.ROLES)
            .build();
    // @formatter:on

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
        var dsl = JooqDSLContext.BEAN.get();

        if (user.id == null) {
            user.id = dsl.nextval(SQ_USER);
        }

        var step = dsl.insertInto(EN_USER)
                .set(EN_USER.ID, user.id);

        if (user.userName != null) {
            step.set(EN_USER.USERNAME, user.userName);
        }
        if (user.password != null) {
            step.set(EN_USER.PASSWORD, user.password);
        }
        if (user.name != null) {
            step.set(EN_USER.NAME, user.name);
        }
        if (user.roles != null) {
            step.set(EN_USER.ROLES, user.roles);
        }

        return step.execute() > 0;
    }

    @Override
    public boolean update(User newBean, User oldBean, User projection) {
        if (newBean == null) {
            throw new AssertionError("newBean is requeried");
        }

        if (newBean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        if (projection == null) {
            projection = this.newProjection();
        }

        var dsl = JooqDSLContext.BEAN.get();
        var step = dsl.update(EN_USER).set(EN_USER.ID, newBean.id);

        boolean hasChanges = false;

        if (changed(newBean, oldBean, projection, u -> u.userName)) {
            step.set(EN_USER.USERNAME, newBean.userName);
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.password)) {
            step.set(EN_USER.PASSWORD, newBean.password);
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.name)) {
            step.set(EN_USER.NAME, newBean.name);
            hasChanges = true;
        }
        if (changed(newBean, oldBean, projection, u -> u.roles)) {
            step.set(EN_USER.ROLES, newBean.roles);
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        return step.where(EN_USER.ID.eq(newBean.id)).execute() > 0;
    }

    @Override
    public int delete(UserCriteria criteria) {
        if (criteria == null || criteria.userId() == null) {
            throw new AssertionError("Missing primary key");
        }

        var dsl = JooqDSLContext.BEAN.get();
        return dsl.deleteFrom(EN_USER)
                .where(applyConditions(EN_USER, criteria))
                .execute();
    }

    @Override
    public int count(UserCriteria criteria) {
        var dsl = JooqDSLContext.BEAN.get();
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

            if (criteria != null && criteria.orderBy() != null) {
                switch (criteria.orderBy()) {
                case ASCENDING -> step.orderBy(t.ID.asc());
                case DESCENDING -> step.orderBy(t.ID.desc());
                }
            }

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
        if (prjBean.id == null) {
            prjBean.id = 0L;
        }

        return QUERY.fetchOne(prjBean, (t, q) -> q.where(t.ID.eq(userId)));
    }

    // :: Internal

    private User projectionFrom(UserCriteria criteria) {
        if (criteria != null && criteria.projection() != null) {
            var prj = criteria.projection();
            if (prj.id == null) {
                prj.id = 0L;
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
            this.ctx = new QueryContext();
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
