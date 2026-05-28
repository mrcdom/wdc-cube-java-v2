package br.com.wdc.shopping.domain.config;

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
 * Resolution order for config file:
 * <ol>
 * <li>System property: {@code shopping.config.file}</li>
 * <li>Default: {@code config/application.toml} relative to working directory</li>
 * </ol>
 * <p>
 * If the file does not exist, the application fails to start.
 */
public final class AppConfig {

    private static final Log LOG = Log.getLogger(AppConfig.class);

    private static final String CONFIG_FILE_PROPERTY = "shopping.config.file";
    private static final String DEFAULT_CONFIG_PATH = "config/application.toml";

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

    public static AppConfig load() {
        var configPath = resolveConfigPath().toAbsolutePath().normalize();
        if (!Files.exists(configPath)) {
            throw new IllegalStateException(
                    "Configuration file not found: " + configPath
                            + ". Create the file or set system property -D" + CONFIG_FILE_PROPERTY + "=<path>");
        }
        LOG.info("Loading configuration from {}", configPath);
        try {
            var content = Files.readString(configPath);
            var props = parseToml(content);
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

    private static Path resolveConfigPath() {
        var configured = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configured != null && !configured.isBlank()) {
            return Paths.get(configured);
        }
        return Paths.get(DEFAULT_CONFIG_PATH);
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
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).strip();
                continue;
            }

            var eqIdx = line.indexOf('=');
            if (eqIdx <= 0) {
                continue;
            }

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

        return result;
    }
}
