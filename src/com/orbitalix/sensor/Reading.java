package com.orbitalix.sensor;

/**
 * One measurement taken from a sensor.
 * Demonstrates: encapsulation (fields are private and final).
 */
public class Reading {

    private final double value;
    private final long timestamp;

    public Reading(double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return value + " @ " + timestamp;
    }
}
