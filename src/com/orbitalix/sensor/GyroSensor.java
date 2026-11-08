package com.orbitalix.sensor;

/**
 * Spin rate used to keep the satellite pointing the right way, in degrees per second.
 * Demonstrates: inheritance, polymorphism.
 */
public class GyroSensor extends Sensor {

    public GyroSensor(String id, String unit, double baseline) {
        super(id, unit, baseline);
        setNoise(0.05);
        setRateLimit(1.5);
        setHighThresholds(4, 8);
        setLowThresholds(-4, -8);
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
