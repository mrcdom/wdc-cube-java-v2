package br.com.wdc.store.persistence.impl;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.SQLDialect;
import org.postgresql.ds.PGSimpleDataSource;

import br.com.wdc.framework.domain.config.AppConfig;

/**
 * Unpooled DataSource for the local views (Vaadin, SWT), read from the same {@code database.*} keys as the backend.
 * <p>
 * The database is chosen by the {@code database.url} URL: PostgreSQL ({@code jdbc:postgresql:}) or H2. Without a URL,
 * it uses a file-based H2 inside {@code dataDir}. The backend does not go through here: it builds the pool in the host.
 * <p>
 * The drivers are optional dependencies of this module; whoever uses this class declares both.
 *
 * @param dataSource DataSource ready for the {@link StoreRepositoryBootstrap}
 * @param dialect    dialect matching the URL
 * @param url        effective JDBC URL (for logging)
 */
public record LocalDataSource(DataSource dataSource, SQLDialect dialect, String url) {

    private static final String DEFAULT_DB_NAME = "wedocode-store";

    public static LocalDataSource fromConfig(AppConfig config, Path dataDir) {
        var url = config.get("database.url");
        if (StringUtils.isBlank(url)) {
            url = "jdbc:h2:file:" + dataDir.resolve(DEFAULT_DB_NAME).toAbsolutePath()
                    + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        }
        var username = config.get("database.username", "sa");
        var password = config.get("database.password", "sa");

        if (url.startsWith("jdbc:postgresql:")) {
            var pg = new PGSimpleDataSource();
            pg.setURL(url);
            pg.setUser(username);
            pg.setPassword(password);
            var schema = config.get("database.schema", null);
            if (StringUtils.isNotBlank(schema)) {
                pg.setCurrentSchema(schema);
            }
            return new LocalDataSource(pg, SQLDialect.POSTGRES, url);
        }

        var h2 = new JdbcDataSource();
        h2.setURL(url);
        h2.setUser(username);
        h2.setPassword(password);
        return new LocalDataSource(h2, SQLDialect.H2, url);
    }
}
