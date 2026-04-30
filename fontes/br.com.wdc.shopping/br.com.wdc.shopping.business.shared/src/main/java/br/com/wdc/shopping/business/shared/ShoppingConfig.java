package br.com.wdc.shopping.business.shared;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import br.com.wdc.shopping.business.shared.config.AppConfig;

public class ShoppingConfig {

    private static Path baseDir;

    private static Path configDir;

    private static Path dataDir;

    private static Path logDir;

    private static Path tempDir;

    private ShoppingConfig() {
        super();
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
            Path baseDir = configuredDir != null && !configuredDir.isBlank()
                    ? Paths.get(configuredDir)
                    : Paths.get("work");
            return createDirectory(baseDir.toAbsolutePath().normalize());
        }

    }

}
