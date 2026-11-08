package com.orbitalix.model;

/**
 * Solar panels and the battery bus.
 * Demonstrates: inheritance, polymorphism.
 */
public class PowerSubsystem extends Subsystem {

    public PowerSubsystem() {
        super("Power");
    }

    @Override
    public HealthStatus evaluateHealth() {
        return rollupSensorHealth();
    }
}
