package com.orbitalix.api;

import java.util.List;

public record SensorSnapshot(String sensorId, double value, String unit, HealthStatus status, List<Double> recent) {
    public SensorSnapshot {
        recent = List.copyOf(recent);
    }
}
