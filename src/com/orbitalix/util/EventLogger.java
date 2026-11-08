package com.orbitalix.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * The one shared log for the whole program. It writes to the console and to logs/orbitalix.log.
 * Demonstrates: Singleton pattern.
 */
public final class EventLogger {

    private static EventLogger instance;

    private final PrintWriter writer;

    private EventLogger() {
        try {
            Path path = Path.of("logs", "orbitalix.log");
            Files.createDirectories(path.getParent());
            writer = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot open orbitalix.log", ex);
        }
    }

    public static synchronized EventLogger getInstance() {
        if (instance == null) {
            instance = new EventLogger();
        }
        return instance;
    }

    public void info(String message) {
        write("INFO", message);
    }

    public void error(String message) {
        write("ERROR", message);
    }

    public void error(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        write("ERROR", name + ": " + throwable.getMessage());
    }

    private void write(String level, String message) {
        String line = level + "  " + message;
        System.out.println(line);
        writer.println(line);
        writer.flush();
    }
}
