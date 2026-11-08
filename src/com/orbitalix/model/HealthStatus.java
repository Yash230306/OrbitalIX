package com.orbitalix.model;

/**
 * Health of one subsystem, shown as a colour on the dashboard.
 * Demonstrates: abstraction.
 */
public enum HealthStatus {
    NOMINAL,
    WARNING,
    CRITICAL,
    FAILED
}
