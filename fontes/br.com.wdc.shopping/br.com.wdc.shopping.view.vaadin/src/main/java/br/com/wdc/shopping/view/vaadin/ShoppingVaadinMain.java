package br.com.wdc.shopping.view.vaadin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.VaadinServlet;

import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.persistence.RepositoryBootstrap;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;

public class ShoppingVaadinMain {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingVaadinMain.class);

    public static void main(String[] args) throws Exception {
        var config = AppConfig.load();
        ShoppingConfig.Internals.configure(config);

        var dataDir = ShoppingConfig.getDataDir();
        var dataSource = new JdbcDataSource();
        dataSource.setURL(resolveJdbcUrl(config, dataDir));
        dataSource.setUser(config.get("database.username", "sa"));
        dataSource.setPassword(config.get("database.password", "sa"));
        SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

        try (var connection = dataSource.getConnection()) {
            var command = new DBCreate().withConnection(connection);
            if (config.getBoolean("database.reset", false)) {
                command.withReset();
            }
            command.run();
        }

        RepositoryBootstrap.initialize();
        LOG.info("Backend initialized with database at {}", dataDir);

        var port = config.getInt("server.port", 8090);
        var server = new Server(port);

        // Use WebAppContext so Vaadin's ServletContainerInitializers are executed
        // (they set up the Lookup, StaticFileHandler, etc.)
        var webapp = new WebAppContext();
        webapp.setContextPath("/");

        // Point WAR to an empty temp dir to avoid scanning our Java 26 .class files
        // (Jetty's embedded ASM does not support class file major version 70)
        var webRoot = Files.createTempDirectory("vaadin-war");
        webapp.setWar(webRoot.toString());

        // Only scan Vaadin/Flow/Atmosphere JARs (compiled with Java 17, ASM compatible)
        // This avoids scanning project JARs/directories with Java 26 bytecode
        webapp.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*vaadin.*\\.jar$|.*flow.*\\.jar$|.*atmosphere.*\\.jar$");

        webapp.setParentLoaderPriority(true);
        webapp.setClassLoader(Thread.currentThread().getContextClassLoader());
        webapp.setThrowUnavailableOnStartupException(true);

        // Discover META-INF/resources/ from classpath JARs (e.g. presentation module
        // images) and add them as extra base resources so servletContext.getResource()
        // can resolve static files like /images/big_logo.png
        var resourceFactory = ResourceFactory.of(webapp);
        var resourceBases = new ArrayList<org.eclipse.jetty.util.resource.Resource>();
        resourceBases.add(resourceFactory.newResource(webRoot));
        var metaInfResources = Thread.currentThread().getContextClassLoader()
                .getResources("META-INF/resources");
        while (metaInfResources.hasMoreElements()) {
            var url = metaInfResources.nextElement();
            resourceBases.add(resourceFactory.newResource(url.toURI()));
            LOG.info("Added static resource base: {}", url);
        }
        webapp.setBaseResource(ResourceFactory.combine(resourceBases));

        // Register VaadinServlet programmatically
        var servlet = new ServletHolder("vaadin", VaadinServlet.class);
        servlet.setInitOrder(1);
        servlet.setInitParameter("pushmode", "automatic");
        webapp.addServlet(servlet, "/*");

        server.setHandler(webapp);
        server.start();

        LOG.info("Vaadin application started on http://localhost:{}", port);
        server.join();
    }

    private static String resolveJdbcUrl(AppConfig config, Path dataDir) {
        var url = config.get("database.url");
        if (url != null && !url.isBlank()) {
            return url;
        }
        return "jdbc:h2:file:" + dataDir.resolve("wedocode-shopping").toAbsolutePath();
    }
}
