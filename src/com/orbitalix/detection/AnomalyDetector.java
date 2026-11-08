package com.orbitalix.detection;

import com.orbitalix.model.Anomaly;
import com.orbitalix.sensor.Sensor;

import java.util.Optional;

/**
 * One algorithm for deciding whether a reading is a problem.
 * Demonstrates: Strategy pattern, abstraction (Interface Segregation).
 */
public interface AnomalyDetector {

    Optional<Anomaly> analyze(Sensor sensor);
}
