package br.com.wdc.shopping.view.vaadin;

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
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.persistence.impl.LocalDataSource;
import br.com.wdc.shopping.persistence.impl.ShoppingRepositoryBootstrap;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;

public class ShoppingVaadinMain {

    private static final Log LOG;

    static {
        Log.setFactory(new Slf4jLogFactory());
        LOG = Log.getLogger(ShoppingVaadinMain.class);
    }

    public static void main(String[] args) throws Exception {
        try (var cleanUp = new Defer()) {

            var config = ShoppingConfig.loadConfig();
            ShoppingConfig.Internals.configure(config);

            CryptoProvider.BEAN.set(new JceCryptoProvider());
            cleanUp.push(() -> CryptoProvider.BEAN.set(null));

            var dataDir = ShoppingConfig.getDataDir();
            var db = LocalDataSource.fromConfig(config, dataDir);

            // Antes do DBCreate: o DBReset (banco novo ou database.reset) grava pelos repositórios
            ShoppingRepositoryBootstrap.initialize(db.dataSource(), config.getBoolean("database.logSql", false),
                    db.dialect(), cleanUp);

            try (var connection = db.dataSource().getConnection()) {
                var command = new DBCreate().withConnection(connection);
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

            // WAR vazio: as classes vêm do classpath, não de um WEB-INF
            var webRoot = Files.createTempDirectory("vaadin-war");
            webapp.setWar(webRoot.toString());

            // Varre só os JARs do Vaadin/Flow/Atmosphere e o local deste módulo (target/classes ou o
            // fat JAR): é por essa varredura que o Vaadin registra o MainLayout como AppShellConfigurator
            // e aplica o tema (@StyleSheet) e o @Push declarados nele
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
