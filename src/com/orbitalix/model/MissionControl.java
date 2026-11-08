package com.orbitalix.model;

import com.orbitalix.alert.AlertListener;
import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.sensor.Sensor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ground software that reads the spacecraft once per tick.
 * Demonstrates: aggregation (it watches a spacecraft it does not own).
 */
public class MissionControl {

    /** Aggregation: the spacecraft is passed in and can exist without this object. */
    private final Spacecraft spacecraft;
    private final List<AlertListener> listeners = new ArrayList<>();
    private OrbitalIXException lastError;

    public MissionControl(Spacecraft spacecraft) {
        this.spacecraft = spacecraft;
    }

    public void addAlertListener(AlertListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeAlertListener(AlertListener listener) {
        listeners.remove(listener);
    }

    /**
     * Read every sensor and update subsystem health.
     * Detection and recovery are added around this step in later packages.
     */
    public void poll() {
        lastError = null;
        for (Subsystem subsystem : spacecraft.getSubsystems()) {
            for (Sensor sensor : subsystem.getSensors()) {
                try {
                    sensor.read();
                } catch (OrbitalIXException ex) {
                    lastError = ex;
                }
            }
            if (subsystem instanceof CommunicationSubsystem communication) {
                try {
                    communication.checkLink();
                } catch (OrbitalIXException ex) {
                    lastError = ex;
                }
            }
        }
        spacecraft.refreshHealth();
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public OrbitalIXException getLastError() {
        return lastError;
    }

    public List<AlertListener> getListeners() {
        return listeners;
    }
}
