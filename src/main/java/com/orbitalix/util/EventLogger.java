package com.orbitalix.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** Singleton event logger writes to the console and an optional local log file. */
public final class EventLogger {
    private static final EventLogger INSTANCE = new EventLogger();
    private EventLogger() {}
    public static EventLogger getInstance() { return INSTANCE; }

    public synchronized void log(String level, String message) {
        String line = Instant.now() + " [" + level + "] " + message;
        System.out.println(line);
        try {
            Path file = Path.of("logs", "orbitalix.log");
            Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Unable to append OrbitalIX log: " + e.getMessage());
        }
    }
}
