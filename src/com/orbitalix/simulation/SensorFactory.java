package com.orbitalix.simulation;

import com.orbitalix.sensor.GyroSensor;
import com.orbitalix.sensor.Sensor;
import com.orbitalix.sensor.SignalSensor;
import com.orbitalix.sensor.TemperatureSensor;
import com.orbitalix.sensor.VoltageSensor;

/**
 * Creates standard satellite sensors with default parameters and baselines.
 * Demonstrates: Factory pattern, encapsulation, abstraction.
 */
public class SensorFactory {

    public static VoltageSensor createVoltageSensor(String id, double baseline) {
        return new VoltageSensor(id, "V", baseline);
    }

    public static TemperatureSensor createTemperatureSensor(String id, double baseline) {
        return new TemperatureSensor(id, "C", baseline);
    }

    public static SignalSensor createSignalSensor(String id, double baseline) {
        return new SignalSensor(id, "%", baseline);
    }

    public static GyroSensor createGyroSensor(String id, double baseline) {
        return new GyroSensor(id, "deg/s", baseline);
    }
}
