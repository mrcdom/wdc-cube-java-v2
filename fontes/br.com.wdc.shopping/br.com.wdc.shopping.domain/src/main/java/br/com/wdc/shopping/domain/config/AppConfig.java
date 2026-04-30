package br.com.wdc.shopping.domain.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application configuration loaded from a TOML file.
 * <p>
 * Resolution order for config file:
 * <ol>
 * <li>System property: {@code shopping.config.file}</li>
 * <li>Default: {@code work/config/application.toml} relative to working directory</li>
 * </ol>
 * <p>
 * If the file does not exist, an empty configuration is used (all values fall back to defaults).
 */
public final class AppConfig {

    private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);

    private static final String CONFIG_FILE_PROPERTY = "shopping.config.file";
    private static final String DEFAULT_CONFIG_PATH = "work/config/application.toml";

    private final Map<String, String> properties;

    private AppConfig(Map<String, String> properties) {
        this.properties = properties;
    }

    public static AppConfig load() {
        var configPath = resolveConfigPath();
        if (Files.exists(configPath)) {
            LOG.info("Loading configuration from {}", configPath.toAbsolutePath());
            try {
                var content = Files.readString(configPath);
                var props = parseToml(content);
                return new AppConfig(props);
            } catch (IOException e) {
                LOG.warn("Failed to read config file {}: {}", configPath, e.getMessage());
            }
        } else {
            LOG.info("No config file found at {}, using defaults", configPath.toAbsolutePath());
        }
        return new AppConfig(Map.of());
    }

    public String get(String key) {
        return properties.get(key);
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

            // Remove surrounding quotes
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            var fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
            result.put(fullKey, value);
        }

        return result;
    }
}
