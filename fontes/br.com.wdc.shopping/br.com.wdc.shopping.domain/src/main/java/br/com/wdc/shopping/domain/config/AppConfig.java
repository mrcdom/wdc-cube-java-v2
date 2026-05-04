package br.com.wdc.shopping.domain.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
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
        var configFile = resolveConfigPath();
        if (configFile.exists()) {
            LOG.info("Loading configuration from {}", configFile.getAbsolutePath());
            try {
                var content = readFileContent(configFile);
                var props = parseToml(content);
                return new AppConfig(props);
            } catch (IOException e) {
                LOG.warn("Failed to read config file {}: {}", configFile, e.getMessage());
            }
        } else {
            LOG.info("No config file found at {}, using defaults", configFile.getAbsolutePath());
        }
        return new AppConfig(Collections.emptyMap());
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

    private static File resolveConfigPath() {
        var configured = System.getProperty(CONFIG_FILE_PROPERTY);
        if (configured != null && !configured.trim().isEmpty()) {
            return new File(configured);
        }
        return new File(DEFAULT_CONFIG_PATH);
    }

    private static String readFileContent(File file) throws IOException {
        var sb = new StringBuilder();
        try (var reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
        }
        return sb.toString();
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
            var line = rawLine.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).trim();
                continue;
            }

            var eqIdx = line.indexOf('=');
            if (eqIdx <= 0) {
                continue;
            }

            var key = line.substring(0, eqIdx).trim();
            var value = line.substring(eqIdx + 1).trim();

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
