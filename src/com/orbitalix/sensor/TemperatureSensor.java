package com.orbitalix.sensor;

/**
 * Core or battery temperature in degrees Celsius.
 * Demonstrates: inheritance, polymorphism.
 */
public class TemperatureSensor extends Sensor {

    public TemperatureSensor(String id, String unit, double baseline) {
        super(id, unit, baseline);
        setNoise(0.25);
        setRateLimit(1.6);
        if (id.contains("BATT")) {
            setHighThresholds(40, 55);
        } else {
            setHighThresholds(45, 70);
        }
    }

    @Override
    public double measure() {
        if (hasActiveBackup()) {
            return getBackup().measure();
        }
        double raw = getBaseline() + noiseSample();
        return applyFaultModel(raw);
    }
}
