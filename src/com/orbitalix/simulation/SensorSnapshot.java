package com.orbitalix.simulation;

import com.orbitalix.model.HealthStatus;

/**
 * Immutable picture of one sensor after one tick.
 * Demonstrates: encapsulation.
 */
public record SensorSnapshot(String sensorId, double value, String unit, HealthStatus status) {
}
