package com.orbitalix.detection;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.sensor.Sensor;

import java.util.Locale;
import java.util.Optional;

/**
 * Flags a temperature that is moving away from normal faster than its rate limit.
 * Demonstrates: Strategy pattern, polymorphism.
 */
public class RateOfChangeDetector implements AnomalyDetector {

    @Override
    public Optional<Anomaly> analyze(Sensor sensor) {
        if (!sensor.getId().startsWith("TEMP") || sensor.getHistory().size() < 2) {
            return Optional.empty();
        }
        double previous = sensor.getPreviousValue();
        double current = sensor.getLastValue();
        double delta = Math.abs(current - previous);
        boolean movingAway = Math.abs(current - sensor.getBaseline())
                > Math.abs(previous - sensor.getBaseline()) + 0.15;
        if (!movingAway || delta <= sensor.getRateLimit()) {
            return Optional.empty();
        }
        Severity severity = delta > sensor.getRateLimit() * 3
                ? Severity.CRITICAL
                : Severity.WARNING;
        String message = String.format(Locale.US,
                "changing too fast (%.1f per tick, limit %.1f)", delta, sensor.getRateLimit());
        return Optional.of(new Anomaly(sensor, severity, message, "RateOfChangeDetector"));
    }
}
