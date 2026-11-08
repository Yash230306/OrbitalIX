package com.orbitalix.simulation;

import com.orbitalix.model.HealthStatus;

import java.util.List;

/**
 * Immutable picture of one subsystem after one tick.
 * Demonstrates: encapsulation.
 */
public record SubsystemSnapshot(String name, HealthStatus status, List<SensorSnapshot> sensors) {
}
