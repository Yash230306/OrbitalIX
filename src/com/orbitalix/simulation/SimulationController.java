package com.orbitalix.simulation;

import com.orbitalix.alert.AlertListener;

import java.util.List;

/**
 * The only door the dashboard uses to reach the simulation.
 * Demonstrates: abstraction, Dependency Inversion, the Controller in MVC.
 */
public interface SimulationController {

    List<String> getScenarioNames();

    List<String> getSubsystemNames();

    void loadScenario(int scenarioIndex);

    void start();

    void pause();

    void reset();

    void setSpeed(int ticksPerSecond);

    void injectFault(String subsystemName, FaultType type);

    void addTickListener(TickListener listener);

    void addAlertListener(AlertListener listener);
}
