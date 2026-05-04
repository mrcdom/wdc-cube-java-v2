package br.com.wdc.shopping.domain;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import br.com.wdc.shopping.domain.config.AppConfig;

public class ShoppingConfig {

    private static File baseDir;

    private static File configDir;

    private static File dataDir;

    private static File logDir;

    private static File tempDir;

    private static String jwtSecret;

    private ShoppingConfig() {
        super();
    }

    public static final File getBaseDir() {
        return baseDir;
    }

    public static final File getConfigDir() {
        return configDir;
    }

    public static final File getDataDir() {
        return dataDir;
    }

    public static final File getLogDir() {
        return logDir;
    }

    public static final File getTempDir() {
        return tempDir;
    }

    public static final String getJwtSecret() {
        return jwtSecret;
    }

    public static class Internals {

        private Internals() {
            super();
        }

        public static void setBaseDir(File path) {
            ShoppingConfig.baseDir = path;
        }

        public static void setConfigDir(File path) {
            ShoppingConfig.configDir = path;
        }

        public static void setDataDir(File path) {
            ShoppingConfig.dataDir = path;
        }

        public static void setLogDir(File path) {
            ShoppingConfig.logDir = path;
        }

        public static void setTempDir(File path) {
            ShoppingConfig.tempDir = path;
        }

        public static void setJwtSecret(String secret) {
            ShoppingConfig.jwtSecret = secret;
        }

        public static void configure(AppConfig config) {
            try {
                File baseDir = resolveRuntimeBaseDir(config);
                File configDir = createDirectory(new File(baseDir, "config"));
                File dataDir = createDirectory(new File(baseDir, "data"));
                File logDir = createDirectory(new File(baseDir, "log"));
                File tempDir = createDirectory(new File(baseDir, "temp"));

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

        private static File createDirectory(File dir) throws IOException {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + dir);
                }
            }
            return dir;
        }

        private static File resolveRuntimeBaseDir(AppConfig config) throws IOException {
            String configuredDir = config.get("app.basedir");
            File baseDir = configuredDir != null && !configuredDir.trim().isEmpty()
                    ? new File(configuredDir)
                    : new File("work");
            return createDirectory(baseDir.getAbsoluteFile());
        }

    }

}
