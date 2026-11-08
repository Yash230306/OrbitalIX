package com.orbitalix.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A scripted flight scenario containing scheduled faults at specific ticks.
 * Demonstrates: encapsulation, abstraction.
 */
public class Scenario {

    public record ScheduledFault(int tick, String subsystemName, FaultType faultType) {}

    private final String name;
    private final String description;
    private final List<ScheduledFault> faults = new ArrayList<>();

    public Scenario(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Scenario addFault(int tick, String subsystemName, FaultType faultType) {
        faults.add(new ScheduledFault(tick, subsystemName, faultType));
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ScheduledFault> getFaults() {
        return Collections.unmodifiableList(faults);
    }

    public List<ScheduledFault> getFaultsForTick(int tick) {
        List<ScheduledFault> matching = new ArrayList<>();
        for (ScheduledFault fault : faults) {
            if (fault.tick() == tick) {
                matching.add(fault);
            }
        }
        return matching;
    }

    public static List<Scenario> defaultScenarios() {
        List<Scenario> list = new ArrayList<>();

        // 1. Nominal run (no faults, all nominal)
        list.add(new Scenario("Nominal Run", "Baseline flight with no faults injected; nominal telemetry."));

        // 2. Solar panel degradation (POWER_DROP)
        Scenario s2 = new Scenario("Solar Panel Degradation", "Bus voltage drops gradually, triggering WARNING state.");
        s2.addFault(5, "Power", FaultType.POWER_DROP);
        list.add(s2);

        // 3. Thermal runaway (OVERHEAT)
        Scenario s3 = new Scenario("Thermal Runaway", "Core temperature rises fast; triggers CRITICAL, enters SAFE MODE, and cools.");
        s3.addFault(5, "Thermal", FaultType.OVERHEAT);
        list.add(s3);

        // 4. Gyro drift (SENSOR_DRIFT)
        Scenario s4 = new Scenario("Gyro Drift", "Attitude gyro begins drifting; caught by ZScoreDetector, switches to backup gyro.");
        s4.addFault(12, "Attitude", FaultType.SENSOR_DRIFT);
        list.add(s4);

        // 5. Communication blackout (DROPOUT)
        Scenario s5 = new Scenario("Communication Blackout", "Antenna signal drops out; CommunicationLossException is handled and logged.");
        s5.addFault(6, "Communication", FaultType.DROPOUT);
        list.add(s5);

        // 6. Cascading failure
        Scenario s6 = new Scenario("Cascading Failure", "Compound failure across Power, Thermal, and Attitude; triggers UnrecoverableFaultException.");
        s6.addFault(5, "Power", FaultType.POWER_DROP);
        s6.addFault(10, "Thermal", FaultType.OVERHEAT);
        s6.addFault(16, "Attitude", FaultType.SENSOR_DRIFT);
        list.add(s6);

        return list;
    }
}
