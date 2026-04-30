package br.com.wdc.shopping.persistence.repository.user;

import java.sql.Connection;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.schema.EnUser;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.domain.criteria.UserCriteria;

public class CountUsersCmd extends BaseCommand {

    public static int byCriteria(Connection connection, UserCriteria criteria) {
        return new CountUsersCmd().execute(connection, criteria);
    }

    // :: Action

    public int execute(Connection connection, UserCriteria criteria) {
        var en = new EnUser("u");

        var sql = new SqlList();
        sql.ln(SELECT, COUNT("*"));
        sql.ln(FROM, en.tableRef());
        sql.ln(WHERE_TRUE);

        var applier = new ApplyUserCriteria(this);
        applier.criteria = criteria;
        applier.root = en;
        applier.apply(sql);

        // Read content
        try (var handle = Jdbi.create(connection).open()) {
            var query = handle.createQuery(sql.toText());
            this.applyParams(query);
            return query.mapTo(Integer.class).one();
        }
    }

}
