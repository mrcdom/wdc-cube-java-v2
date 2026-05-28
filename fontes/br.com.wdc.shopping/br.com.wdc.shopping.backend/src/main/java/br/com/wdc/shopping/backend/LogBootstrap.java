package br.com.wdc.shopping.backend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Inicializa o Logback a partir de um arquivo externo {@code logback.xml}.
 *
 * <p>
 * Resolução do arquivo de configuração:
 * </p>
 * <ol>
 * <li>System property: {@code logback.configurationFile}</li>
 * <li>Default: {@code work/config/logback.xml} relativo ao diretório de trabalho</li>
 * </ol>
 *
 * <p>
 * Se o arquivo não existir, uma versão padrão é criada automaticamente.
 * Deve ser chamado antes de qualquer logging (tipicamente no início do {@code main}).
 * </p>
 */
final class LogBootstrap {

    private static final String CONFIG_PROPERTY = "logback.configurationFile";
    private static final String DEFAULT_CONFIG_PATH = "work/config/logback.xml";

    private LogBootstrap() {
    }

    /**
     * Configura o Logback a partir do arquivo externo.
     * Se o arquivo não existir, cria um com conteúdo padrão.
     */
    static void initialize() {
        Path configFile = resolveConfigFile();
        ensureFileExists(configFile);
        reconfigure(configFile);
    }

    private static Path resolveConfigFile() {
        String configured = System.getProperty(CONFIG_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(DEFAULT_CONFIG_PATH).toAbsolutePath().normalize();
    }

    private static void ensureFileExists(Path configFile) {
        if (Files.exists(configFile)) {
            return;
        }
        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, DEFAULT_LOGBACK_XML);
        } catch (IOException e) {
            System.err.println("[LogBootstrap] Failed to write default logback.xml: " + e.getMessage());
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
            System.err.println("[LogBootstrap] Failed to configure logback from " + configFile + ": " + e.getMessage());
        }
    }

    private static final String DEFAULT_LOGBACK_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <configuration>

            	<property name="LOG_DIR" value="work/log" />
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

            	<!-- jOOQ SQL Logger (ativado por database.logSql=true) -->
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
