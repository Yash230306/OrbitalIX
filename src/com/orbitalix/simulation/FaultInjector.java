package com.orbitalix.simulation;

import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.sensor.Sensor;
import com.orbitalix.util.EventLogger;

/**
 * Injects physical failures and sensor anomalies into the satellite.
 * Demonstrates: abstraction, method overloading (inject across Spacecraft, Subsystem, Sensor).
 */
public class FaultInjector {

    /**
     * Injects a fault into a named subsystem of a spacecraft.
     */
    public void inject(Spacecraft spacecraft, String subsystemName, FaultType type) {
        Subsystem subsystem = spacecraft.findSubsystem(subsystemName);
        if (subsystem != null) {
            inject(subsystem, type);
        } else {
            EventLogger.getInstance().warn("Cannot inject fault: unknown subsystem " + subsystemName);
        }
    }

    /**
     * Injects a fault into the most appropriate sensor of a subsystem.
     */
    public void inject(Subsystem subsystem, FaultType type) {
        Sensor target = subsystem.getPrimarySensor();
        if (type == FaultType.OVERHEAT) {
            Sensor core = subsystem.findSensor("TEMP-CORE");
            if (core != null) {
                target = core;
            }
        } else if (type == FaultType.POWER_DROP) {
            Sensor bus = subsystem.findSensor("VOLT-BUS");
            if (bus != null) {
                target = bus;
            }
        }
        if (target != null) {
            inject(target, type);
            EventLogger.getInstance().warn("Fault injected on " + subsystem.getName() + " -> " + type);
        }
    }

    /**
     * Injects a fault directly into a sensor.
     */
    public void inject(Sensor sensor, FaultType type) {
        switch (type) {
            case SENSOR_DRIFT -> {
                if (sensor.getId().startsWith("GYRO")) {
                    sensor.applyDrift(0.18);
                } else {
                    sensor.applyDrift(0.5);
                }
            }
            case SPIKE -> sensor.applySpike(35.0);
            case STUCK_VALUE -> sensor.applyStuck();
            case DROPOUT -> sensor.applyDropout();
            case OVERHEAT -> sensor.applyRampToward(3.2, 85.0);
            case POWER_DROP -> sensor.applyRampToward(-0.6, 18.0);
        }
    }
}
