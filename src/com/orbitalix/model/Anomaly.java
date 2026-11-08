package com.orbitalix.model;

import com.orbitalix.sensor.Sensor;

/**
 * Something the detectors noticed on one sensor.
 * Demonstrates: association (an anomaly points at a sensor but does not own it), encapsulation.
 */
public class Anomaly {

    /** Association: the sensor already exists on the spacecraft; this object only refers to it. */
    private final Sensor sensor;
    private final Severity severity;
    private final String message;
    private final long timestamp;
    private final String detectorName;

    public Anomaly(Sensor sensor, Severity severity, String message) {
        this(sensor, severity, message, "detector", System.currentTimeMillis());
    }

    public Anomaly(Sensor sensor, Severity severity, String message, String detectorName) {
        this(sensor, severity, message, detectorName, System.currentTimeMillis());
    }

    public Anomaly(Sensor sensor, Severity severity, String message, String detectorName, long timestamp) {
        this.sensor = sensor;
        this.severity = severity;
        this.message = message;
        this.detectorName = detectorName;
        this.timestamp = timestamp;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDetectorName() {
        return detectorName;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + sensor.getId() + " " + message;
    }
}
