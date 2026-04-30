package br.com.wdc.shopping.persistence.repository.user;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;

import com.google.gson.stream.JsonReader;

import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.schema.EnUser;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.DbField;
import br.com.wdc.shopping.persistence.sql.SqlUtils;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class FetchUsersCmd extends BaseCommand {

    public static User byId(Connection connection, Long userId, User projection) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }

        var list = new FetchUsersCmd().execute(connection, new UserCriteria()
                .withUserId(userId)
                .withProjection(projection));

        return list.isEmpty() ? null : list.get(0);

    }

    public static List<User> byCriteria(Connection connection, UserCriteria criteria) {
        return new FetchUsersCmd().execute(connection, criteria);
    }

    // :: Action

    public List<User> execute(Connection connection, UserCriteria criteria) {
        var sql = new SqlList();

        var cteUser = new EnUser("cteUser");
        sql.ln(WITH, cteUser.alias(), AS, '(');
        sql.ln(this.cteUser(criteria, criteria.projection(), null, null).toText("  "));
        sql.ln(')');
        sql.ln(SELECT);

        var fields = fields(criteria.projection(), cteUser);
        var fJsonData = sql.strColumn(SqlUtils.toJsonField(fields), AS, "json_data");
        sql.ln(FROM, cteUser.alias());

        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);

            var userMap = new HashMap<Long, User>();
            return query.map((rs, _) -> fromJson(fJsonData.apply(rs), userMap)).list();
        }
    }

    public SqlList cteUser(UserCriteria criteria, User prj, String superAlias, DbField superId) {
        var u = new EnUser("U");

        var sql = new SqlList();
        sql.ln(SELECT);
        fields(prj, u).forEach(sql::field);
        sql.ln(FROM, u.tableRef());
        sql.ln(WHERE_TRUE);

        if (superAlias != null) {
            sql.ln(AND, EXISTS(ll -> ll
                    .ln(SELECT, 1)
                    .ln(FROM, superAlias)
                    .ln(WHERE, superId, EQUAL, u.id)));
        }

        if (criteria == null) {
            return sql;
        }

        var applier = new ApplyUserCriteria(this);
        applier.criteria = criteria;
        applier.root = u;
        applier.apply(sql);

        if (criteria.orderBy() != null) {
            switch (criteria.orderBy()) {
            case ACENDING -> sql.ln(ORDER_BY(u.id.asc()));
            case DESCENDING -> sql.ln(ORDER_BY(u.id.desc()));
            }
        }

        if (criteria.limit() != null) {
            sql.ln(LIMIT, criteria.limit());
        }

        if (criteria.offset() != null) {
            sql.ln(OFFSET, criteria.offset());
        }

        return sql;
    }

    // :: Public Class API

    public static List<DbField> fields(User prj, EnUser en) {
        var pv = ProjectionValues.INSTANCE;

        if (prj == null) {
            prj = new User();
            prj.name = pv.str;
            prj.userName = pv.str;
            prj.name = pv.str;
        }

        prj.id = pv.i64;

        var fields = new ArrayList<DbField>();
        if (prj.id != null) {
            fields.add(en.id);
        }

        if (prj.userName != null) {
            fields.add(en.userName);
        }

        if (prj.password != null) {
            fields.add(en.password);
        }

        if (prj.name != null) {
            fields.add(en.name);
        }

        return fields;
    }

    public static User fromJson(String json, Map<Long, User> userMap) {
        try (var reader = new JsonReader(new StringReader(json))) {
            var row = EnUser.Row.parseJson(reader);

            var user = userMap.computeIfAbsent(row.id(), k -> {
                var bean = new User();
                bean.id = k;
                return bean;
            });

            if (user.userName == null) {
                user.userName = row.userName();
            }

            if (user.password == null) {
                user.password = row.password();
            }

            if (user.name == null) {
                user.name = row.name();
            }
            return user;
        } catch (IOException caught) {
            throw new UncheckedIOException(caught);
        }
    }

}
