package br.com.wdc.framework.domain.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import br.com.wdc.framework.commons.log.Log;

/**
 * Application configuration loaded from a TOML file.
 * <p>
 * Resolution order for the config file (see {@link #load(String, String)}):
 * <ol>
 * <li>System property whose key is provided by the application</li>
 * <li>Default path provided by the application, relative to the working directory</li>
 * </ol>
 * <p>
 * If the file does not exist, the application fails to start.
 * <p>
 * After loading the main file, {@code application.local.toml} in the same directory is
 * loaded if present and its keys override the main config. Use this file for secrets
 * (passwords, tokens) that must not be committed to version control — add it to
 * {@code .gitignore}.
 */
public final class AppConfig {

    private static final Log LOG = Log.getLogger(AppConfig.class);

    /** Name of the optional, version-control-ignored override file loaded alongside the main config. */
    private static final String LOCAL_OVERRIDE_FILE = "application.local.toml";

    private final Map<String, String> properties;
    private final Path configFilePath;

    private AppConfig(Map<String, String> properties, Path configFilePath) {
        this.properties = properties;
        this.configFilePath = configFilePath;
    }

    /**
     * Returns the absolute path of the loaded configuration file.
     */
    public Path getConfigFilePath() {
        return configFilePath;
    }

    /**
     * Returns the directory containing the configuration file.
     */
    public Path getConfigFileDir() {
        return configFilePath.getParent();
    }

    /**
     * Loads configuration from a TOML file.
     *
     * @param configFilePropertyKey system property consulted first for the config file path
     * @param defaultConfigPath     fallback path (relative to the working directory) when the
     *                              system property is absent
     */
    public static AppConfig load(String configFilePropertyKey, String defaultConfigPath) {
        var configPath = resolveConfigPath(configFilePropertyKey, defaultConfigPath).toAbsolutePath().normalize();
        if (!Files.exists(configPath)) {
            throw new IllegalStateException(
                    "Configuration file not found: " + configPath
                            + ". Create the file or set system property -D" + configFilePropertyKey + "=<path>");
        }
        LOG.info("Loading configuration from {}", configPath);
        try {
            var content = Files.readString(configPath);
            var props = new LinkedHashMap<>(parseToml(content));

            // Load optional local override file (not tracked by version control)
            var localPath = configPath.resolveSibling(LOCAL_OVERRIDE_FILE);
            if (Files.exists(localPath)) {
                LOG.info("Loading local overrides from {}", localPath);
                props.putAll(parseToml(Files.readString(localPath)));
            }

            return new AppConfig(props, configPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + configPath, e);
        }
    }

    public String get(String key) {
        return properties.get(key);
    }

    public AppConfig withOverride(String key, String value) {
        var copy = new LinkedHashMap<>(this.properties);
        copy.put(key, value);
        return new AppConfig(copy, this.configFilePath);
    }

    public String get(String key, String defaultValue) {
        var value = properties.get(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer value for key '{}': '{}'", key, value);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        var value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value);
    }

    private static Path resolveConfigPath(String configFilePropertyKey, String defaultConfigPath) {
        var configured = System.getProperty(configFilePropertyKey);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(defaultConfigPath);
    }

    /**
     * Minimal TOML parser supporting:
     * <ul>
     * <li>Sections: [section]</li>
     * <li>Key-value pairs: key = value / key = "value"</li>
     * <li>Comments: # ...</li>
     * <li>Nested sections: [section.subsection]</li>
     * </ul>
     * Keys are flattened as "section.key".
     */
    static Map<String, String> parseToml(String content) {
        var result = new LinkedHashMap<String, String>();
        var currentSection = "";

        for (var rawLine : content.split("\n")) {
            var line = rawLine.strip();

            if (line.isEmpty() || line.startsWith("#")) {
                // skip comments and blank lines
            } else if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).strip();
            } else {
                var eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    var key = line.substring(0, eqIdx).strip();
                    var value = line.substring(eqIdx + 1).strip();

                    // Remove surrounding quotes (double or single)
                    if (value.length() >= 2
                            && ((value.startsWith("\"") && value.endsWith("\""))
                                    || (value.startsWith("'") && value.endsWith("'")))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    var fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    result.put(fullKey, value);
                }
            }
        }

        return result;
    }
}
