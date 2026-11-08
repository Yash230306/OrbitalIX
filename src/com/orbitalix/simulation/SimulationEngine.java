package com.orbitalix.simulation;

import com.orbitalix.alert.AlertListener;
import com.orbitalix.model.HealthStatus;
import com.orbitalix.model.MissionControl;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.sensor.Sensor;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates one simulation tick: applies scenario faults, steps mission control,
 * captures an immutable snapshot, and notifies listeners.
 * Demonstrates: Observer pattern, aggregation, encapsulation.
 */
public class SimulationEngine {

    private Spacecraft spacecraft;
    private MissionControl missionControl;
    private final FaultInjector faultInjector = new FaultInjector();
    private Scenario scenario;
    private int currentTick;

    private final List<TickListener> tickListeners = new ArrayList<>();
    private final List<AlertListener> alertListeners = new ArrayList<>();

    public SimulationEngine() {
        this(Scenario.defaultScenarios().get(0));
    }

    public SimulationEngine(Scenario scenario) {
        this.scenario = scenario;
        reset();
    }

    public synchronized SimulationSnapshot tick() {
        currentTick++;

        if (scenario != null) {
            for (Scenario.ScheduledFault fault : scenario.getFaultsForTick(currentTick)) {
                faultInjector.inject(spacecraft, fault.subsystemName(), fault.faultType());
            }
        }

        missionControl.watch();

        SimulationSnapshot snapshot = createSnapshot();
        for (TickListener listener : new ArrayList<>(tickListeners)) {
            listener.onTick(snapshot);
        }
        return snapshot;
    }

    public synchronized void reset() {
        this.currentTick = 0;
        this.spacecraft = SubsystemFactory.createSpacecraft();
        this.missionControl = new MissionControl(spacecraft);
        for (AlertListener listener : alertListeners) {
            missionControl.addAlertListener(listener);
        }
    }

    public synchronized void publishCurrentSnapshot() {
        SimulationSnapshot snapshot = createSnapshot();
        for (TickListener listener : new ArrayList<>(tickListeners)) {
            listener.onTick(snapshot);
        }
    }

    public synchronized void loadScenario(Scenario scenario) {
        this.scenario = scenario;
        reset();
    }

    public void injectFault(String subsystemName, FaultType type) {
        faultInjector.inject(spacecraft, subsystemName, type);
    }

    public void addTickListener(TickListener listener) {
        tickListeners.add(listener);
    }

    public void removeTickListener(TickListener listener) {
        tickListeners.remove(listener);
    }

    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
        if (missionControl != null) {
            missionControl.addAlertListener(listener);
        }
    }

    public void removeAlertListener(AlertListener listener) {
        alertListeners.remove(listener);
        if (missionControl != null) {
            missionControl.removeAlertListener(listener);
        }
    }

    public SimulationSnapshot createSnapshot() {
        List<SubsystemSnapshot> subSnapshots = new ArrayList<>();
        for (Subsystem sub : spacecraft.getSubsystems()) {
            List<SensorSnapshot> sensorSnapshots = new ArrayList<>();
            for (Sensor s : sub.getSensors()) {
                HealthStatus sensorHealth = HealthStatus.NOMINAL;
                if (s.isFailed()) {
                    sensorHealth = HealthStatus.FAILED;
                } else if (s.isBeyondCritical()) {
                    sensorHealth = HealthStatus.CRITICAL;
                } else if (s.isBeyondWarning()) {
                    sensorHealth = HealthStatus.WARNING;
                }
                sensorSnapshots.add(new SensorSnapshot(s.getId(), s.getLastValue(), s.getUnit(), sensorHealth));
            }
            subSnapshots.add(new SubsystemSnapshot(sub.getName(), sub.getHealth(), sensorSnapshots));
        }
        return new SimulationSnapshot(currentTick, spacecraft.getModeName(), subSnapshots);
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public MissionControl getMissionControl() {
        return missionControl;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public int getCurrentTick() {
        return currentTick;
    }
}
