package com.orbitalix.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Singleton settings store; persisted under the user's home directory. */
public final class Settings {
    private static final Settings INSTANCE = new Settings();
    private final Properties values = new Properties();
    private final Path file = Path.of(System.getProperty("user.home"), ".orbitalix", "settings.properties");

    private Settings() {
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                values.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load settings from " + file, e);
            }
        }
    }

    public static Settings getInstance() { return INSTANCE; }
    public synchronized String get(String key, String fallback) { return values.getProperty(key, fallback); }
    public synchronized void put(String key, String value) {
        values.setProperty(key, value);
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                values.store(output, "OrbitalIX settings");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save settings to " + file, e);
        }
    }
}
