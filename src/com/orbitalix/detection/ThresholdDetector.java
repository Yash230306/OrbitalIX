package com.orbitalix.detection;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.sensor.Sensor;

import java.util.Locale;
import java.util.Optional;

/**
 * Flags a reading that has crossed a fixed warning or critical limit.
 * Demonstrates: Strategy pattern, polymorphism.
 */
public class ThresholdDetector implements AnomalyDetector {

    @Override
    public Optional<Anomaly> analyze(Sensor sensor) {
        if (sensor.getHistory().isEmpty()) {
            return Optional.empty();
        }
        String reading = String.format(Locale.US, "%.1f %s", sensor.getLastValue(), sensor.getUnit());
        if (sensor.isBeyondCritical()) {
            return Optional.of(new Anomaly(sensor, Severity.CRITICAL,
                    "crossed a critical limit (" + reading + ")", "ThresholdDetector"));
        }
        if (sensor.isBeyondWarning()) {
            return Optional.of(new Anomaly(sensor, Severity.WARNING,
                    "crossed a warning limit (" + reading + ")", "ThresholdDetector"));
        }
        return Optional.empty();
    }
}
