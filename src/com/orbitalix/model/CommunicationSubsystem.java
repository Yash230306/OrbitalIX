package com.orbitalix.model;

import com.orbitalix.exception.CommunicationLossException;
import com.orbitalix.sensor.Sensor;

/**
 * The antenna that talks to the ground.
 * Demonstrates: inheritance, polymorphism, exception handling.
 */
public class CommunicationSubsystem extends Subsystem {

    private boolean lossReported;

    public CommunicationSubsystem() {
        super("Communication");
    }

    @Override
    public HealthStatus evaluateHealth() {
        return rollupSensorHealth();
    }

    /**
     * A silent antenna is a lost link. The exception is thrown once per dropout
     * so the log shows it without flooding.
     */
    public void checkLink() throws CommunicationLossException {
        Sensor antenna = getPrimarySensor();
        if (antenna != null && antenna.isDropout()) {
            if (!lossReported) {
                lossReported = true;
                throw new CommunicationLossException("Uplink lost on " + antenna.getId());
            }
        } else {
            lossReported = false;
        }
    }
}
