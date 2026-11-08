package com.orbitalix.detection;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.sensor.Reading;
import com.orbitalix.sensor.Sensor;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Compares a gyro reading with the average of its early, healthy samples.
 * A huge z-score means the gyro is drifting.
 * Demonstrates: Strategy pattern, polymorphism.
 */
public class ZScoreDetector implements AnomalyDetector {

    /** How many early samples form the healthy baseline. */
    private static final int BASELINE_SAMPLES = 10;

    /** A calm gyro jumps around by about 0.05, so this is a very large miss. */
    private static final double Z_LIMIT = 30;

    @Override
    public Optional<Anomaly> analyze(Sensor sensor) {
        if (!sensor.getId().startsWith("GYRO") || sensor.isUsingBackup()) {
            return Optional.empty();
        }
        List<Reading> history = sensor.getHistory();
        if (history.size() < BASELINE_SAMPLES + 2) {
            return Optional.empty();
        }
        double mean = 0;
        for (int i = 0; i < BASELINE_SAMPLES; i++) {
            mean += history.get(i).getValue();
        }
        mean /= BASELINE_SAMPLES;
        double variance = 0;
        for (int i = 0; i < BASELINE_SAMPLES; i++) {
            double diff = history.get(i).getValue() - mean;
            variance += diff * diff;
        }
        double std = Math.sqrt(variance / BASELINE_SAMPLES);
        if (std < 0.05) {
            std = 0.05;
        }
        double z = (sensor.getLastValue() - mean) / std;
        if (Math.abs(z) < Z_LIMIT) {
            return Optional.empty();
        }
        String message = String.format(Locale.US,
                "statistically unusual (z-score %.1f), drift suspected", z);
        return Optional.of(new Anomaly(sensor, Severity.WARNING, message, "ZScoreDetector"));
    }
}
