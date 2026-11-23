package br.com.wdc.cube.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Initializes Logback from an external {@code logback.xml} file.
 *
 * <p>
 * Resolution of the working directory (basedir):
 * </p>
 * <ol>
 * <li>Locates {@code application.toml} via the system property
 * {@code shopping.config.file} or the default {@code config/application.toml}</li>
 * <li>Reads {@code app.basedir} (required) and resolves it relative to the file's location</li>
 * </ol>
 *
 * <p>
 * {@code logback.xml} is looked up at {@code {basedir}/config/logback.xml}.
 * If the file does not exist, a default version is created automatically.
 * Must be called before any logging (typically at the start of {@code main}).
 * </p>
 */
final class LogBootstrap {

    private static final System.Logger BOOTSTRAP_LOG = System.getLogger(LogBootstrap.class.getName());

    private static final String CONFIG_FILE_PROPERTY = "shopping.config.file";
    private static final String DEFAULT_CONFIG_PATH = "config/application.toml";
    private static final Pattern BASEDIR_PATTERN = Pattern.compile("^\\s*basedir\\s*=\\s*[\"']([^\"']+)[\"']");

    private LogBootstrap() {
    }

    /**
     * Configures Logback from the external file.
     * If the file does not exist, creates one with default content.
     */
    static void initialize() {
        Path baseDir = resolveBaseDir();
        Path configFile = baseDir.resolve("config/logback.xml");
        ensureFileExists(configFile, baseDir);
        reconfigure(configFile);
    }

    private static Path resolveBaseDir() {
        Path configFile = resolveAppConfigFile();
        if (!Files.exists(configFile)) {
            throw new IllegalStateException(
                    "[LogBootstrap] Configuration file not found: " + configFile.toAbsolutePath()
                            + ". Create the file or set -D" + CONFIG_FILE_PROPERTY + "=<path>");
        }
        String basedir = peekBasedir(configFile);
        if (basedir == null) {
            throw new IllegalStateException(
                    "[LogBootstrap] Property 'app.basedir' is required in " + configFile.toAbsolutePath());
        }
        // Resolve basedir relative to the config file's directory
        return configFile.toAbsolutePath().getParent().resolve(basedir).normalize();
    }

    private static Path resolveAppConfigFile() {
        String configured = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        return Paths.get(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize();
    }

    /**
     * Reads only the {@code app.basedir} value from the TOML, with no external dependencies.
     */
    private static String peekBasedir(Path tomlFile) {
        if (!Files.exists(tomlFile)) {
            return null;
        }
        try {
            boolean inAppSection = false;
            for (String line : Files.readAllLines(tomlFile)) {
                String trimmed = line.trim();
                if (trimmed.startsWith("[")) {
                    inAppSection = trimmed.equals("[app]");
                    continue;
                }
                if (inAppSection) {
                    var matcher = BASEDIR_PATTERN.matcher(line);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (IOException e) {
            // silently ignore — will use default
        }
        return null;
    }

    private static void ensureFileExists(Path configFile, Path baseDir) {
        if (Files.exists(configFile)) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            String logDir = baseDir.resolve("log").toString().replace("\\", "/");
            Files.writeString(configFile, DEFAULT_LOGBACK_XML.replace("${DEFAULT_LOG_DIR}", logDir));
        } catch (IOException e) {
            BOOTSTRAP_LOG.log(System.Logger.Level.ERROR, "[LogBootstrap] Failed to write default logback.xml: " + e.getMessage());
        }
    }

    private static void reconfigure(Path configFile) {
        try {
            var context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            var configurator = new JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(configFile.toFile());
        } catch (JoranException e) {
            BOOTSTRAP_LOG.log(System.Logger.Level.ERROR, "[LogBootstrap] Failed to configure logback from " + configFile + ": " + e.getMessage());
        }
    }

    private static final String DEFAULT_LOGBACK_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>

                <property name="LOG_DIR" value="${DEFAULT_LOG_DIR}" />
                <property name="LOG_PATTERN" value="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n" />

                <!-- Console Appender -->
                <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
                    <encoder>
                        <pattern>${LOG_PATTERN}</pattern>
                    </encoder>
                </appender>

                <!-- File Appender -->
                <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
                    <file>${LOG_DIR}/server.log</file>
                    <encoder>
                        <pattern>${LOG_PATTERN}</pattern>
                    </encoder>
                    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
                        <fileNamePattern>${LOG_DIR}/server.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
                        <maxFileSize>10MB</maxFileSize>
                        <maxHistory>7</maxHistory>
                    </rollingPolicy>
                </appender>

                <!-- Javalin Logger -->
                <logger name="io.javalin" level="INFO" />

                <!-- Jetty Logger -->
                <logger name="org.eclipse.jetty" level="WARN" />

                <!-- jOOQ SQL Logger (enabled by database.logSql=true) -->
                <logger name="org.jooq.tools.LoggerListener" level="DEBUG" />

                <!-- WeDoCode Loggers -->
                <logger name="br.com.wdc" level="DEBUG" />

                <!-- Root Logger -->
                <root level="INFO">
                    <appender-ref ref="CONSOLE" />
                    <appender-ref ref="FILE" />
                </root>

            </configuration>
            """;
}
