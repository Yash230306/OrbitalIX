package com.orbitalix.model;

import com.orbitalix.sensor.Sensor;
import com.orbitalix.sensor.TemperatureSensor;

/**
 * Core and battery temperatures. In safe mode the core is allowed to cool.
 * Demonstrates: inheritance, polymorphism.
 */
public class ThermalSubsystem extends Subsystem {

    public ThermalSubsystem() {
        super("Thermal");
    }

    @Override
    public HealthStatus evaluateHealth() {
        return rollupSensorHealth();
    }

    /** Stop the heater and let the core radiate heat back toward its baseline. */
    public void beginCooling() {
        for (Sensor sensor : getSensors()) {
            if (sensor instanceof TemperatureSensor temperature) {
                temperature.beginCooling();
            }
        }
    }
}
