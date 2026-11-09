package com.orbitalix.api;

import java.util.List;

public record SubsystemSnapshot(String name, HealthStatus status, List<SensorSnapshot> sensors, List<String> activeFaults) {
    public SubsystemSnapshot {
        sensors = List.copyOf(sensors);
        activeFaults = List.copyOf(activeFaults);
    }
}
