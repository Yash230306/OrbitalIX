package com.orbitalix.model;

import com.orbitalix.sensor.Sensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One part of the spacecraft (power, thermal, communication or attitude).
 * Demonstrates: abstraction, encapsulation, composition (sensors belong to the subsystem).
 */
public abstract class Subsystem {

    private final String name;
    /** Composition: these sensors are created for this subsystem and are not shared. */
    private final List<Sensor> sensors = new ArrayList<>();
    private HealthStatus health = HealthStatus.NOMINAL;
    private int restartIn = -1;

    protected Subsystem(String name) {
        this.name = name;
    }

    protected Subsystem(String name, List<Sensor> sensors) {
        this.name = name;
        for (Sensor sensor : sensors) {
            addSensor(sensor);
        }
    }

    public void addSensor(Sensor sensor) {
        sensor.setSubsystemName(name);
        sensors.add(sensor);
    }

    public abstract HealthStatus evaluateHealth();

    /** Worst status among the sensors. Subclasses call this from evaluateHealth(). */
    protected final HealthStatus rollupSensorHealth() {
        HealthStatus worst = HealthStatus.NOMINAL;
        for (Sensor sensor : sensors) {
            HealthStatus current = statusOf(sensor);
            if (current.ordinal() > worst.ordinal()) {
                worst = current;
            }
        }
        this.health = worst;
        return health;
    }

    private HealthStatus statusOf(Sensor sensor) {
        if (sensor.isFailed()) {
            return HealthStatus.FAILED;
        }
        if (sensor.isBeyondCritical()) {
            return HealthStatus.CRITICAL;
        }
        if (sensor.isBeyondWarning()) {
            return HealthStatus.WARNING;
        }
        return HealthStatus.NOMINAL;
    }

    /** Clears sensor faults after a short delay so the dashboard can show the failure first. */
    public void scheduleRestart(int ticks) {
        if (restartIn < 0) {
            restartIn = ticks;
        }
    }

    public void tickMaintenance() {
        if (restartIn > 0) {
            restartIn--;
            if (restartIn == 0) {
                for (Sensor sensor : sensors) {
                    sensor.clearFaults();
                }
                restartIn = -1;
            }
        }
    }

    public void forceHealth(HealthStatus health) {
        this.health = health;
    }

    public String getName() {
        return name;
    }

    public HealthStatus getHealth() {
        return health;
    }

    public List<Sensor> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    public Sensor getPrimarySensor() {
        if (sensors.isEmpty()) {
            return null;
        }
        return sensors.get(0);
    }

    public Sensor findSensor(String sensorId) {
        for (Sensor sensor : sensors) {
            if (sensor.getId().equals(sensorId)) {
                return sensor;
            }
        }
        return null;
    }
}
