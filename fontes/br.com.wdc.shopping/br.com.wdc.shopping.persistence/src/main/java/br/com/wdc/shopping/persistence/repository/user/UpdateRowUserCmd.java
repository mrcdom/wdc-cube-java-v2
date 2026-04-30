package br.com.wdc.shopping.persistence.repository.user;

import java.sql.Connection;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;

import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.persistence.repository.BaseCommand;
import br.com.wdc.shopping.persistence.schema.EnUser;
import br.com.wdc.shopping.persistence.sql.SqlList;
import br.com.wdc.shopping.persistence.sql.SqlUtils;

public class UpdateRowUserCmd extends BaseCommand {

    public static boolean run(Connection connection, User bean) {
        if (bean.id == null) {
            throw new AssertionError("Missing primary key");
        }

        return new UpdateRowUserCmd().execute(connection, rowFromBean(bean)) > 0;
    }

    public static boolean run(Connection connection, User newBean, User oldBean) {
        if (newBean.id == null) {
            throw new AssertionError("Missing primary key in newUser");
        }

        if (oldBean.id == null) {
            throw new AssertionError("Missing primary key in oldUser");
        }

        if (!Objects.equals(newBean.id, oldBean.id)) {
            throw new AssertionError("New and old bean must have some key value");
        }

        var row = rowFromBean(oldBean);
        row.clearChanges();

        var hasChanges = false;

        if (Objects.equals(row.userName(), newBean.userName)) {
            row.userName(newBean.userName);
            hasChanges = true;
        }

        if (Objects.equals(row.password(), newBean.password)) {
            row.password(newBean.password);
            hasChanges = true;
        }

        if (Objects.equals(row.name(), newBean.name)) {
            row.name(newBean.name);
            hasChanges = true;
        }

        if (hasChanges) {
            return new UpdateRowUserCmd().execute(connection, row) > 0;
        }

        return false;
    }

    public int execute(Connection connection, EnUser.Row row) {
        var en = EnUser.INSTANCE;

        var sql = new SqlList();

        sql.ln(UPDATE, en.tableName(), SET);

        var comma = SqlUtils.comma();
        if (row.isUserNameChanged()) {
            sql.ln(comma.get(), en.userName, EQUAL, param("userName", row.userName()));
        }

        if (row.isPasswordChanged()) {
            sql.ln(comma.get(), en.password, EQUAL, param("password", row.password()));
        }

        if (row.isNameChanged()) {
            sql.ln(comma.get(), en.name, EQUAL, param("name", row.name()));
        }

        if (paramsIsEmpty()) {
            return 0;
        }

        if (row.id() != null) {
            sql.ln(WHERE, en.id, EQUAL, param("id", row.id()));
        } else {
            throw new AssertionError("Missing primary key");
        }

        try (var handle = Jdbi.create(connection).open()) {
            var update = handle.createUpdate(sql.toText());
            this.applyParams(update);
            return update.execute();
        }
    }

    private static EnUser.Row rowFromBean(User bean) {
        var row = new EnUser.Row();
        row.id(bean.id);
        row.userName(bean.userName);
        row.password(bean.password);
        row.name(bean.name);
        return row;
    }

}
