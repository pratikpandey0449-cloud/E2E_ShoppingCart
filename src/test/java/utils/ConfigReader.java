package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ConfigReader {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream inputStream = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                throw new RuntimeException("config.properties not found in classpath");
            }
            PROPERTIES.load(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to load config.properties", ex);
        }
    }

    // Prevents utility class instantiation.
    private ConfigReader() {
    }

    // Resolves config values in priority order: system property, environment variable, then file.
    public static String get(String key) {
        // Runtime overrides take priority so CI can swap values without editing the repo.
        String value = System.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        
        // Environment variables use CONFIG_KEY style names for portability.
        String envKey = key.replace('.', '_').toUpperCase();
        value = System.getenv(envKey);
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }

        // Fall back to the checked-in defaults for local runs.
        value = PROPERTIES.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing config key: " + key + " (and no matching env var " + envKey + ")");
        }
        return value.trim();
    }

    // Returns the resolved config value as an integer.
    public static int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    // Returns the resolved config value as a boolean.
    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }
}
