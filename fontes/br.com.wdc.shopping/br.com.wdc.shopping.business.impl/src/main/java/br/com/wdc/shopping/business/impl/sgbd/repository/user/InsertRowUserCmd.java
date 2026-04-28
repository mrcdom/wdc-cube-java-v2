package br.com.wdc.shopping.business.impl.sgbd.repository.user;

import java.sql.Connection;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.business.impl.sgbd.ddl.tables.EnUser;
import br.com.wdc.shopping.business.impl.sgbd.dsl.SqlList;
import br.com.wdc.shopping.business.impl.sgbd.utils.BaseCommand;
import br.com.wdc.shopping.business.shared.model.User;

public class InsertRowUserCmd extends BaseCommand {

    public static boolean run(Connection connection, User bean) {
        var row = new EnUser.Row();
        row.id(bean.id);

        if (bean.userName != null) {
            row.userName(bean.userName);
        }

        if (bean.password != null) {
            row.password(bean.password);
        }

        if (bean.name != null) {
            row.name(bean.name);
        }
        
        var inserted = new InsertRowUserCmd().execute(connection, row) > 0;
        bean.id = row.id();
        return inserted;
    }

    public int execute(Connection connection, EnUser.Row row) {
        var en = EnUser.INSTANCE;

        if (row.id() == null) {
            row.id(en.nextSeqUser(connection));
        }

        var sql = new SqlList();
        var places = new ArrayList<String>();

        sql.ln(INSERT_INTO, en.tableName(), '(');
        sql.ln(' ', en.id);
        places.add(param("id", row.id()));

        if (row.isUserNameChanged()) {
            sql.ln(',', en.userName);
            places.add(param("userName", row.userName()));
        }

        if (row.isPasswordChanged()) {
            sql.ln(',', en.password);
            places.add(param("password", row.password()));
        }

        if (row.isNameChanged()) {
            sql.ln(',', en.name);
            places.add(param("name", row.name()));
        }

        sql.add(")");

        sql.add(VALUES);
        sql.add("(" + StringUtils.join(places, ",") + ")");

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

}
