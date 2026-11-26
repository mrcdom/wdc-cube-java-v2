package br.com.wdc.cube.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Initializes Logback from {@code config/logback.xml} of the work directory ({@code StoreConfig}), creating a default
 * one when it does not exist. The file finds the work directory's {@code log/} folder in the {@code LOG_DIR} property,
 * which this sets. Must be called before any logging (at the start of {@code main}).
 */
final class LogBootstrap {

    private static final System.Logger BOOTSTRAP_LOG = System.getLogger(LogBootstrap.class.getName());

    private LogBootstrap() {
    }

    /** Configures Logback from {@code workDir/config/logback.xml}. */
    static void initialize(Path workDir) {
        Path configFile = workDir.resolve("config/logback.xml");
        ensureFileExists(configFile);
        reconfigure(configFile, workDir.resolve("log"));
    }

    private static void ensureFileExists(Path configFile) {
        if (Files.exists(configFile)) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, DEFAULT_LOGBACK_XML);
        } catch (IOException e) {
            BOOTSTRAP_LOG.log(System.Logger.Level.ERROR, "[LogBootstrap] Failed to write default logback.xml: " + e.getMessage());
        }
    }

    private static void reconfigure(Path configFile, Path logDir) {
        try {
            var context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            context.putProperty("LOG_DIR", logDir.toString().replace("\\", "/"));
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

                <!-- LOG_DIR: the work directory's log/ folder, given by LogBootstrap -->
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
