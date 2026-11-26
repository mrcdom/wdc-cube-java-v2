package br.com.wdc.store.view.vaadin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.ResourceFactory;

import com.vaadin.flow.server.VaadinServlet;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.log.Slf4jLogFactory;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.store.domain.StoreConfig;
import br.com.wdc.store.persistence.impl.LocalDataSource;
import br.com.wdc.store.persistence.impl.StoreRepositoryBootstrap;
import br.com.wdc.store.scripts.sgbd.DBCreate;

public class StoreVaadinMain {

    private static final Log LOG;

    static {
        Log.setFactory(new Slf4jLogFactory());
        LOG = Log.getLogger(StoreVaadinMain.class);
    }

    /** Entry point: {@code [--workdir=<folder>]}, or {@code STORE_WORKDIR} ({@link StoreConfig}). */
    public static void main(String[] args) throws Exception {
        Path workDir;
        try {
            workDir = StoreConfig.resolveWorkDir(args, System.getenv());
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(2);
            return;
        }
        try (var cleanUp = new Defer()) {

            var config = StoreConfig.Internals.load(workDir);

            CryptoProvider.BEAN.set(new JceCryptoProvider());
            cleanUp.push(() -> CryptoProvider.BEAN.set(null));

            var dataDir = StoreConfig.getDataDir();
            var db = LocalDataSource.fromConfig(config, dataDir);

            // Before DBCreate: DBReset (new database or database.reset) writes through the repositories
            StoreRepositoryBootstrap.initialize(db.dataSource(), config.getBoolean("database.logSql", false),
                    db.dialect(), cleanUp);

            try (var connection = db.dataSource().getConnection()) {
                var command = new DBCreate()
                        .withConnection(connection);
                if (config.getBoolean("database.reset", false)) {
                    command.withReset();
                }
                command.run();
            }

            LOG.info("Backend initialized with database {}", db.url());

            var port = config.getInt("server.port", 8090);
            var server = new Server(port);

            // Use WebAppContext so Vaadin's ServletContainerInitializers are executed
            // (they set up the Lookup, StaticFileHandler, etc.)
            var webapp = new WebAppContext();
            webapp.setContextPath("/");

            // Empty WAR: the classes come from the classpath, not from a WEB-INF
            var webRoot = Files.createTempDirectory("vaadin-war");
            webapp.setWar(webRoot.toString());

            // Scans only the Vaadin/Flow/Atmosphere JARs and this module's location (target/classes or the
            // fat JAR): this scan is how Vaadin registers MainLayout as AppShellConfigurator
            // and applies the theme (@StyleSheet) and the @Push declared on it
            var ownPath = Path.of(MainLayout.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            var ownLocation = ".*" + Pattern.quote(ownPath.toString()) + "/?$";
            webapp.setAttribute(
                    "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                    ".*vaadin.*\\.jar$|.*flow.*\\.jar$|.*atmosphere.*\\.jar$|" + ownLocation);

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
            cleanUp.push(server::stop);

            LOG.info("Vaadin application started on http://localhost:{}", port);
            server.join();
        }
    }
}
