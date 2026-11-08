package com.orbitalix.simulation;

/**
 * Faults an operator, or a scripted scenario, can inject.
 * Demonstrates: abstraction.
 */
public enum FaultType {
    SENSOR_DRIFT,
    SPIKE,
    STUCK_VALUE,
    DROPOUT,
    OVERHEAT,
    POWER_DROP
}
