package com.orbitalix.detection;

import com.orbitalix.model.Anomaly;
import com.orbitalix.sensor.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holds the detectors and asks each of them about a sensor.
 * The worst anomaly wins, so a critical limit beats a warning about rate.
 * Demonstrates: aggregation, Strategy pattern, Open/Closed (add a detector, do not edit this class).
 */
public class DetectionEngine {

    /** Aggregation: detectors are supplied from outside and can be swapped. */
    private final List<AnomalyDetector> detectors = new ArrayList<>();

    public DetectionEngine(List<AnomalyDetector> detectors) {
        this.detectors.addAll(detectors);
    }

    public void addDetector(AnomalyDetector detector) {
        detectors.add(detector);
    }

    public Optional<Anomaly> analyze(Sensor sensor) {
        Anomaly worst = null;
        for (AnomalyDetector detector : detectors) {
            Optional<Anomaly> found = detector.analyze(sensor);
            if (found.isEmpty()) {
                continue;
            }
            Anomaly anomaly = found.get();
            if (worst == null || anomaly.getSeverity().ordinal() > worst.getSeverity().ordinal()) {
                worst = anomaly;
            }
        }
        if (worst == null) {
            return Optional.empty();
        }
        return Optional.of(worst);
    }

    public static DetectionEngine withDefaults() {
        return new DetectionEngine(List.of(
                new ThresholdDetector(),
                new RateOfChangeDetector(),
                new ZScoreDetector()));
    }
}
