package com.orbitalix.sensor;

/**
 * Antenna signal strength, as a percentage.
 * Demonstrates: inheritance, polymorphism.
 */
public class SignalSensor extends Sensor {

    public SignalSensor(String id, String unit, double baseline) {
        super(id, unit, baseline);
        setNoise(0.45);
        setRateLimit(25);
        setLowThresholds(60, 20);
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
