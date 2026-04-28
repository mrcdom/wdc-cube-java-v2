package br.com.wdc.shopping.view.react;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.h2.tools.Server;

/**
 * Starts H2 in TCP server mode pointing to the same database file used by the application.
 * <p>
 * Connect from any database IDE using:
 * <pre>
 *   URL:      jdbc:h2:tcp://localhost:9092/file:&lt;absolute-path&gt;/work/data/wedocode-shopping
 *   User:     sa
 *   Password: (empty)
 * </pre>
 * <p>
 * Also starts a web console at http://localhost:8082 for quick browser-based access.
 */
public class H2ServerMain {

    private static final String DEFAULT_DB_NAME = "wedocode-shopping";

    public static void main(String[] args) throws Exception {
        Path dataDir = resolveDataDir();
        Path dbFile = dataDir.resolve(DEFAULT_DB_NAME).toAbsolutePath();

        String baseDir = dataDir.toAbsolutePath().toString();

        Server tcpServer = Server.createTcpServer(
                "-tcp",
                "-tcpAllowOthers",
                "-tcpPort", "9092",
                "-baseDir", baseDir
        ).start();

        Server webServer = Server.createWebServer(
                "-web",
                "-webAllowOthers",
                "-webPort", "8082",
                "-baseDir", baseDir
        ).start();

        System.out.println("==========================================================");
        System.out.println(" H2 Database Server started");
        System.out.println("==========================================================");
        System.out.println(" TCP Server : " + tcpServer.getURL());
        System.out.println(" Web Console: " + webServer.getURL());
        System.out.println();
        System.out.println(" JDBC URL   : jdbc:h2:tcp://localhost:9092/file:" + dbFile);
        System.out.println(" User       : sa");
        System.out.println(" Password   : (empty)");
        System.out.println("==========================================================");
        System.out.println(" Press Ctrl+C to stop.");
        System.out.println("==========================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tcpServer.stop();
            webServer.stop();
            System.out.println("H2 servers stopped.");
        }));

        Thread.currentThread().join();
    }

    private static Path resolveDataDir() {
        String configuredDir = System.getProperty("wedocode.shopping.runtime.dir");
        Path baseDir = configuredDir != null && !configuredDir.isBlank()
                ? Paths.get(configuredDir)
                : Paths.get("work");
        return baseDir.toAbsolutePath().normalize().resolve("data");
    }
}
