package br.com.wdc.shopping.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import br.com.wdc.framework.domain.config.AppConfig;

public class ShoppingConfig {

    /** System property consulted first for the config file location. */
    private static final String CONFIG_FILE_PROPERTY = "shopping.config.file";

    /** Default config path, relative to the working directory. */
    private static final String DEFAULT_CONFIG_PATH = "config/application.toml";

    private static Path baseDir;

    private static Path configDir;

    private static Path dataDir;

    private static Path logDir;

    private static Path tempDir;

    private static String jwtSecret;

    private ShoppingConfig() {
        super();
    }

    /**
     * Carrega a configuração da aplicação aplicando as convenções do Shopping
     * (system property {@code shopping.config.file} → {@code config/application.toml}).
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

    public static class Internals {

        private Internals() {
            super();
        }

        public static void setBaseDir(Path path) {
            ShoppingConfig.baseDir = path;
        }

        public static void setConfigDir(Path path) {
            ShoppingConfig.configDir = path;
        }

        public static void setDataDir(Path path) {
            ShoppingConfig.dataDir = path;
        }

        public static void setLogDir(Path path) {
            ShoppingConfig.logDir = path;
        }

        public static void setTempDir(Path path) {
            ShoppingConfig.tempDir = path;
        }

        public static void setJwtSecret(String secret) {
            ShoppingConfig.jwtSecret = secret;
        }

        public static void configure(AppConfig config) {
            try {
                Path baseDir = resolveRuntimeBaseDir(config);
                Path configDir = createDirectory(baseDir.resolve("config"));
                Path dataDir = createDirectory(baseDir.resolve("data"));
                Path logDir = createDirectory(baseDir.resolve("log"));
                Path tempDir = createDirectory(baseDir.resolve("temp"));

                ShoppingConfig.Internals.setBaseDir(baseDir);
                ShoppingConfig.Internals.setConfigDir(configDir);
                ShoppingConfig.Internals.setDataDir(dataDir);
                ShoppingConfig.Internals.setLogDir(logDir);
                ShoppingConfig.Internals.setTempDir(tempDir);

                var jwt = config.get("security.jwt.secret");
                ShoppingConfig.Internals.setJwtSecret(jwt);
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
