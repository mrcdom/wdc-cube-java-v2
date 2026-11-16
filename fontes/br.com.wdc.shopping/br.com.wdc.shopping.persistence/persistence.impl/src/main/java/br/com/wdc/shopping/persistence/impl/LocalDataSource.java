package br.com.wdc.shopping.persistence.impl;

import java.nio.file.Path;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.SQLDialect;
import org.postgresql.ds.PGSimpleDataSource;

import br.com.wdc.framework.domain.config.AppConfig;

/**
 * DataSource sem pool para as views locais (Vaadin, SWT), lido das mesmas chaves {@code database.*} do backend.
 * <p>
 * O banco é escolhido pela URL de {@code database.url}: PostgreSQL ({@code jdbc:postgresql:}) ou H2. Sem URL,
 * usa o H2 em arquivo dentro de {@code dataDir}. O backend não passa por aqui: monta o pool no host.
 * <p>
 * Os drivers são dependências opcionais deste módulo; quem usa esta classe declara os dois.
 *
 * @param dataSource DataSource pronto para o {@link ShoppingRepositoryBootstrap}
 * @param dialect    dialeto correspondente à URL
 * @param url        URL JDBC efetiva (para log)
 */
public record LocalDataSource(DataSource dataSource, SQLDialect dialect, String url) {

    private static final String DEFAULT_DB_NAME = "wedocode-shopping";

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
