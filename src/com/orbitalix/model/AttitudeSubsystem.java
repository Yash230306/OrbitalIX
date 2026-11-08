package com.orbitalix.model;

/**
 * Gyroscopes that keep the satellite pointed the right way.
 * Demonstrates: inheritance, polymorphism.
 */
public class AttitudeSubsystem extends Subsystem {

    public AttitudeSubsystem() {
        super("Attitude");
    }

    @Override
    public HealthStatus evaluateHealth() {
        return rollupSensorHealth();
    }
}
