package br.com.wdc.shopping.business.shared;

import java.nio.file.Path;

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

    }

}
