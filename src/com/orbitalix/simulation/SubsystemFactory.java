package com.orbitalix.simulation;

import com.orbitalix.model.AttitudeSubsystem;
import com.orbitalix.model.CommunicationSubsystem;
import com.orbitalix.model.PowerSubsystem;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.model.ThermalSubsystem;
import com.orbitalix.sensor.GyroSensor;

import java.util.List;

/**
 * Creates configured satellite subsystems and default spacecraft.
 * Demonstrates: Factory pattern, composition (assembles sensors into subsystems, subsystems into spacecraft).
 */
public class SubsystemFactory {

    public static PowerSubsystem createPowerSubsystem() {
        PowerSubsystem power = new PowerSubsystem();
        power.addSensor(SensorFactory.createVoltageSensor("VOLT-BUS", 28.0));
        power.addSensor(SensorFactory.createVoltageSensor("VOLT-SOLAR", 32.0));
        return power;
    }

    public static ThermalSubsystem createThermalSubsystem() {
        ThermalSubsystem thermal = new ThermalSubsystem();
        thermal.addSensor(SensorFactory.createTemperatureSensor("TEMP-CORE", 25.0));
        thermal.addSensor(SensorFactory.createTemperatureSensor("TEMP-BATT", 20.0));
        return thermal;
    }

    public static CommunicationSubsystem createCommunicationSubsystem() {
        CommunicationSubsystem comm = new CommunicationSubsystem();
        comm.addSensor(SensorFactory.createSignalSensor("SIG-ANT", 96.0));
        return comm;
    }

    public static AttitudeSubsystem createAttitudeSubsystem() {
        AttitudeSubsystem attitude = new AttitudeSubsystem();
        GyroSensor primary = SensorFactory.createGyroSensor("GYRO-PRI", 0.0);
        GyroSensor backup = SensorFactory.createGyroSensor("GYRO-BKP", 0.0);
        primary.setBackup(backup);
        attitude.addSensor(primary);
        return attitude;
    }

    public static List<Subsystem> createAllSubsystems() {
        return List.of(
                createPowerSubsystem(),
                createThermalSubsystem(),
                createCommunicationSubsystem(),
                createAttitudeSubsystem()
        );
    }

    public static Spacecraft createSpacecraft() {
        return new Spacecraft("OrbitalIX-1", createAllSubsystems());
    }

    public static Spacecraft createSpacecraft(String name) {
        return new Spacecraft(name, createAllSubsystems());
    }
}
