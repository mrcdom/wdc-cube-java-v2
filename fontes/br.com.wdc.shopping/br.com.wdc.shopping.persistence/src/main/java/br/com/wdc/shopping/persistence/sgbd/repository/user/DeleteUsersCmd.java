package br.com.wdc.shopping.persistence.sgbd.repository.user;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;
import br.com.wdc.shopping.persistence.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.domain.criteria.UserCriteria;

public class DeleteUsersCmd extends BaseCommand {

    public static int byId(Connection connection, Long userId) {
        if (userId == null) {
            throw new AssertionError("userId is required");
        }
        return new DeleteUsersCmd().execute(connection, new UserCriteria()
                .withUserId(userId));
    }

    public static int byCriteria(Connection connection, UserCriteria criteria) {
        return new DeleteUsersCmd().execute(connection, criteria);
    }

    public int execute(Connection connection, UserCriteria criteria) {
        var en = new EnUser("u");

        var sql = new SqlList();
        sql.ln(DELETE);
        sql.ln(FROM, en.tableName());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyUserCriteria(this);
        applier.criteria = criteria;
        applier.root = en;
        applier.apply(sql);

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
