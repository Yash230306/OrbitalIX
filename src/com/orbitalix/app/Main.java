package com.orbitalix.app;

import com.orbitalix.alert.ConsoleAlertListener;
import com.orbitalix.alert.FileAlertListener;
import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.model.AttitudeSubsystem;
import com.orbitalix.model.CommunicationSubsystem;
import com.orbitalix.model.MissionControl;
import com.orbitalix.model.PowerSubsystem;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.model.ThermalSubsystem;
import com.orbitalix.sensor.GyroSensor;
import com.orbitalix.sensor.Reading;
import com.orbitalix.sensor.SignalSensor;
import com.orbitalix.sensor.TemperatureSensor;
import com.orbitalix.sensor.VoltageSensor;

import java.util.List;

/**
 * Entry point. A1 checks that a spacecraft can be built and read.
 * CLI and the dashboard replace this body in later tasks.
 * Demonstrates: objects, constructors, method calls.
 */
public class Main {

    public static void main(String[] args) {
        PowerSubsystem power = new PowerSubsystem();
        power.addSensor(new VoltageSensor("VOLT-BUS", "V", 28));
        ThermalSubsystem thermal = new ThermalSubsystem();
        thermal.addSensor(new TemperatureSensor("TEMP-CORE", "C", 25));
        CommunicationSubsystem communication = new CommunicationSubsystem();
        communication.addSensor(new SignalSensor("SIG-ANT", "%", 96));
        AttitudeSubsystem attitude = new AttitudeSubsystem();
        GyroSensor gyro = new GyroSensor("GYRO-PRI", "deg/s", 0);
        gyro.setBackup(new GyroSensor("GYRO-BKP", "deg/s", 0));
        attitude.addSensor(gyro);

        List<Subsystem> subsystems = List.of(power, thermal, communication, attitude);
        Spacecraft spacecraft = new Spacecraft(subsystems);
        MissionControl control = new MissionControl(spacecraft);
        control.addAlertListener(new ConsoleAlertListener());
        control.addAlertListener(new FileAlertListener());

        control.watch();
        TemperatureSensor core = (TemperatureSensor) thermal.getPrimarySensor();
        core.applyRamp(3);
        System.out.println("OrbitalIX A3 smoke test");
        System.out.println("Spacecraft: " + spacecraft.getName() + " mode " + spacecraft.getModeName());
        for (int tick = 1; tick <= 28; tick++) {
            control.watch();
            Reading latest = core.getHistory().get(core.getHistory().size() - 1);
            System.out.printf("tick %d  TEMP-CORE %5.1f C  thermal %-8s  mode %s%n",
                    tick, latest.getValue(), thermal.getHealth(), spacecraft.getModeName());
        }
        if (control.getLastError() instanceof OrbitalIXException) {
            System.out.println("Last error: " + control.getLastError().getMessage());
        }
        System.out.println("A3 smoke test OK");
    }
}
