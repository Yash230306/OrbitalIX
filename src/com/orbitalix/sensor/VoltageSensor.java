package com.orbitalix.sensor;

/**
 * Solar-array or battery-bus voltage.
 * Demonstrates: inheritance, polymorphism.
 */
public class VoltageSensor extends Sensor {

    public VoltageSensor(String id, String unit, double baseline) {
        super(id, unit, baseline);
        setNoise(0.12);
        setRateLimit(0.9);
        if (id.contains("SOLAR")) {
            setLowThresholds(27, 22);
        } else {
            setLowThresholds(24, 20);
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
