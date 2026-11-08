package com.orbitalix.alert;

import com.orbitalix.model.Anomaly;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Appends each anomaly to logs/alerts.log.
 * Demonstrates: Observer pattern, polymorphism.
 */
public class FileAlertListener implements AlertListener {

    private final PrintWriter writer;

    public FileAlertListener() {
        this(Path.of("logs", "alerts.log"));
    }

    public FileAlertListener(Path path) {
        try {
            Files.createDirectories(path.getParent());
            writer = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot open alert log " + path, ex);
        }
    }

    @Override
    public void onAnomaly(Anomaly anomaly) {
        writer.println(anomaly.toString());
        writer.flush();
    }
}
