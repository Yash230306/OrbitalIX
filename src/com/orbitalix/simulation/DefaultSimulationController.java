package com.orbitalix.simulation;

import com.orbitalix.alert.AlertListener;
import com.orbitalix.model.Subsystem;

import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller connecting the simulation engine with the GUI and CLI.
 * Drives the simulation using a javax.swing.Timer so GUI updates remain on the Event Dispatch Thread.
 * Demonstrates: Controller (MVC), Dependency Inversion, Observer pattern.
 */
public class DefaultSimulationController implements SimulationController {

    private final SimulationEngine engine;
    private final List<Scenario> scenarios;
    private final Timer timer;
    private int ticksPerSecond = 2;

    public DefaultSimulationController() {
        this.scenarios = Scenario.defaultScenarios();
        this.engine = new SimulationEngine(scenarios.isEmpty() ? null : scenarios.get(0));
        this.timer = new Timer(1000 / ticksPerSecond, e -> engine.tick());
    }

    public DefaultSimulationController(SimulationEngine engine, List<Scenario> scenarios) {
        this.engine = engine;
        this.scenarios = scenarios;
        this.timer = new Timer(1000 / ticksPerSecond, e -> engine.tick());
    }

    @Override
    public List<String> getScenarioNames() {
        List<String> names = new ArrayList<>();
        for (Scenario s : scenarios) {
            names.add(s.getName());
        }
        return names;
    }

    @Override
    public List<String> getSubsystemNames() {
        List<String> names = new ArrayList<>();
        for (Subsystem sub : engine.getSpacecraft().getSubsystems()) {
            names.add(sub.getName());
        }
        return names;
    }

    @Override
    public void loadScenario(int scenarioIndex) {
        if (scenarioIndex >= 0 && scenarioIndex < scenarios.size()) {
            pause();
            engine.loadScenario(scenarios.get(scenarioIndex));
            // Trigger tick 0 snapshot update
            publishCurrentSnapshot();
        }
    }

    @Override
    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    @Override
    public void pause() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    @Override
    public void reset() {
        pause();
        engine.reset();
        publishCurrentSnapshot();
    }

    @Override
    public void setSpeed(int ticksPerSecond) {
        this.ticksPerSecond = Math.max(1, ticksPerSecond);
        timer.setDelay(1000 / this.ticksPerSecond);
    }

    @Override
    public void injectFault(String subsystemName, FaultType type) {
        engine.injectFault(subsystemName, type);
    }

    @Override
    public void addTickListener(TickListener listener) {
        engine.addTickListener(listener);
    }

    @Override
    public void addAlertListener(AlertListener listener) {
        engine.addAlertListener(listener);
    }

    public void publishCurrentSnapshot() {
        engine.publishCurrentSnapshot();
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }
}
