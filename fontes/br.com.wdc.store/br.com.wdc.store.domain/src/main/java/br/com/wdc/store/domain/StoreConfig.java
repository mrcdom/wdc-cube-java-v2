package br.com.wdc.store.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import br.com.wdc.framework.domain.config.AppConfig;

public class StoreConfig {

    /** System property consulted first for the config file location. */
    private static final String CONFIG_FILE_PROPERTY = "store.config.file";

    /** Default config path, relative to the working directory. */
    private static final String DEFAULT_CONFIG_PATH = "config/application.toml";

    private static Path baseDir;

    private static Path configDir;

    private static Path dataDir;

    private static Path logDir;

    private static Path tempDir;

    private static String jwtSecret;

    private static int authRequestsPerMinute = 60;

    private StoreConfig() {
        super();
    }

    /**
     * Loads the application configuration applying the WDC Store conventions
     * (system property {@code store.config.file} → {@code config/application.toml}).
     */
    public static AppConfig loadConfig() {
        return AppConfig.load(CONFIG_FILE_PROPERTY, DEFAULT_CONFIG_PATH);
    }

    public static final Path getBaseDir() {
        return baseDir;
    }

    public static final Path getConfigDir() {
        return configDir;
    }

    public static final Path getDataDir() {
        return dataDir;
    }

    public static final Path getLogDir() {
        return logDir;
    }

    public static final Path getTempDir() {
        return tempDir;
    }

    public static final String getJwtSecret() {
        return jwtSecret;
    }

    /** Requests to {@code /api/auth} (challenge, login, refresh) per client address and minute; {@code 0} = no limit. */
    public static final int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }

    public static class Internals {

        private Internals() {
            super();
        }

        public static void setBaseDir(Path path) {
            StoreConfig.baseDir = path;
        }

        public static void setConfigDir(Path path) {
            StoreConfig.configDir = path;
        }

        public static void setDataDir(Path path) {
            StoreConfig.dataDir = path;
        }

        public static void setLogDir(Path path) {
            StoreConfig.logDir = path;
        }

        public static void setTempDir(Path path) {
            StoreConfig.tempDir = path;
        }

        public static void setJwtSecret(String secret) {
            StoreConfig.jwtSecret = secret;
        }

        public static void setAuthRequestsPerMinute(int limit) {
            StoreConfig.authRequestsPerMinute = limit;
        }

        public static void configure(AppConfig config) {
            try {
                Path baseDir = resolveRuntimeBaseDir(config);
                Path configDir = createDirectory(baseDir.resolve("config"));
                Path dataDir = createDirectory(baseDir.resolve("data"));
                Path logDir = createDirectory(baseDir.resolve("log"));
                Path tempDir = createDirectory(baseDir.resolve("temp"));

                StoreConfig.Internals.setBaseDir(baseDir);
                StoreConfig.Internals.setConfigDir(configDir);
                StoreConfig.Internals.setDataDir(dataDir);
                StoreConfig.Internals.setLogDir(logDir);
                StoreConfig.Internals.setTempDir(tempDir);

                var jwt = config.get("security.jwt.secret");
                StoreConfig.Internals.setJwtSecret(jwt);
                StoreConfig.Internals.setAuthRequestsPerMinute(config.getInt("security.auth.requestsPerMinute", 60));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static Path createDirectory(Path dir) throws IOException {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            return dir;
        }

        private static Path resolveRuntimeBaseDir(AppConfig config) throws IOException {
            String configuredDir = config.get("app.basedir");
            if (configuredDir == null || configuredDir.isBlank()) {
                throw new IllegalStateException(
                        "Property 'app.basedir' is required in " + config.getConfigFilePath());
            }
            // Resolve relative to the config file's directory
            Path resolved = config.getConfigFileDir().resolve(configuredDir).normalize();
            return createDirectory(resolved);
        }

    }

}
