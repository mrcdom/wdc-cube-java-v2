package br.com.wdc.store.domain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import br.com.wdc.framework.domain.config.AppConfig;

/**
 * Where a WDC Store host (the backend, Vaadin, SWT) finds everything it uses: an explicit <b>work directory</b>, given
 * by {@code --workdir=<folder>} or the {@value #WORKDIR_ENV} variable. The folder must exist; there is no default
 * relative to the process, so the same binary behaves the same wherever it is started from. Its subfolders are fixed:
 *
 * <ul>
 * <li>{@code config/} — {@code application.toml}, {@code application.local.toml} and {@code logback.xml}</li>
 * <li>{@code data/} — the local database and other data kept between runs</li>
 * <li>{@code log/} — the log files</li>
 * <li>{@code temp/} — temporary files</li>
 * <li>{@code frontend/} — one subfolder per context of static files served (the web clients)</li>
 * </ul>
 *
 * In development the work directory is the repository's {@code work/}. Code refers to the folders through this class,
 * never to a fixed {@code work/...} path.
 */
public final class StoreConfig {

    /** The argument that gives the work directory: {@code --workdir=<folder>}. */
    public static final String WORKDIR_ARG = "--workdir=";

    /** The environment variable that gives the work directory, when the argument is absent. */
    public static final String WORKDIR_ENV = "STORE_WORKDIR";

    private static Path workDir;

    private static Path configDir;

    private static Path dataDir;

    private static Path logDir;

    private static Path tempDir;

    private static Path frontendDir;

    private static AppConfig config;

    private static String jwtSecret;

    private static int authRequestsPerMinute = 60;

    private StoreConfig() {
        super();
    }

    /**
     * The work directory given by {@code args} ({@value #WORKDIR_ARG}) or, without it, by {@code env}
     * ({@value #WORKDIR_ENV}).
     *
     * @throws IllegalStateException when neither gives one, or the folder does not exist
     */
    public static Path resolveWorkDir(String[] args, Map<String, String> env) {
        String given = null;
        for (var arg : args) {
            if (arg.startsWith(WORKDIR_ARG)) {
                given = arg.substring(WORKDIR_ARG.length());
            }
        }
        if (given == null || given.isBlank()) {
            given = env.get(WORKDIR_ENV);
        }
        if (given == null || given.isBlank()) {
            throw new IllegalStateException("No work directory: start with " + WORKDIR_ARG + "<folder> or set "
                    + WORKDIR_ENV + " (in development, the repository's work/ folder)");
        }
        var dir = Path.of(given).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("The work directory does not exist: " + dir);
        }
        return dir;
    }

    /** The arguments without {@value #WORKDIR_ARG}: what the host reads itself (the port, for the backend). */
    public static String[] withoutWorkDir(String[] args) {
        return Arrays.stream(args)
                .filter(arg -> !arg.startsWith(WORKDIR_ARG))
                .toArray(String[]::new);
    }

    /** The configuration loaded from the work directory ({@link Internals#load}). */
    public static AppConfig getConfig() {
        return config;
    }

    public static Path getWorkDir() {
        return workDir;
    }

    public static Path getConfigDir() {
        return configDir;
    }

    public static Path getDataDir() {
        return dataDir;
    }

    public static Path getLogDir() {
        return logDir;
    }

    public static Path getTempDir() {
        return tempDir;
    }

    public static Path getFrontendDir() {
        return frontendDir;
    }

    public static String getJwtSecret() {
        return jwtSecret;
    }

    /** Requests to {@code /api/auth} (challenge, login, refresh) per client address and minute; {@code 0} = no limit. */
    public static int getAuthRequestsPerMinute() {
        return authRequestsPerMinute;
    }

    public static class Internals {

        private Internals() {
            super();
        }

        /** Points every folder at the subfolders of {@code dir}, without reading any configuration. */
        public static void setWorkDir(Path dir) {
            StoreConfig.workDir = dir;
            StoreConfig.configDir = dir.resolve("config");
            StoreConfig.dataDir = dir.resolve("data");
            StoreConfig.logDir = dir.resolve("log");
            StoreConfig.tempDir = dir.resolve("temp");
            StoreConfig.frontendDir = dir.resolve("frontend");
        }

        public static void setJwtSecret(String secret) {
            StoreConfig.jwtSecret = secret;
        }

        public static void setAuthRequestsPerMinute(int limit) {
            StoreConfig.authRequestsPerMinute = limit;
        }

        /**
         * Loads {@code config/application.toml} of {@code dir} and points every folder at it; {@code data/},
         * {@code log/} and {@code temp/} are created when missing.
         */
        public static AppConfig load(Path dir) {
            setWorkDir(dir);
            var loaded = AppConfig.load(configDir.resolve("application.toml"));
            try {
                Files.createDirectories(dataDir);
                Files.createDirectories(logDir);
                Files.createDirectories(tempDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            StoreConfig.config = loaded;
            setJwtSecret(loaded.get("security.jwt.secret"));
            setAuthRequestsPerMinute(loaded.getInt("security.auth.requestsPerMinute", 60));
            return loaded;
        }
    }

}
