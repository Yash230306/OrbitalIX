package com.orbitalix.simulation;

import com.orbitalix.api.*;
import com.orbitalix.config.*;
import com.orbitalix.util.Settings;

import java.util.List;
import java.util.Objects;

/** Facade that exposes a stable API around the simulation engine. */
public final class DefaultSimulationController implements SimulationController {
    private final SimulationEngine engine;

    public DefaultSimulationController() { this(new SimulationEngine()); }
    public DefaultSimulationController(SimulationEngine engine) { this.engine = Objects.requireNonNull(engine); }

    @Override public List<String> getScenarioNames() { return engine.scenarioNames(); }
    @Override public List<String> getFaultTargets() {
        return List.of("Propulsion", "Power", "Thermal", "Communication", "Attitude", "Life Support", "Navigation", "Computer", "Environment");
    }
    @Override public MissionConfigOptions getConfigOptions() {
        return new MissionConfigOptions(
                List.of(new OptionInfo("MOON", "80 distance units; 1 tick uplink"),
                        new OptionInfo("MARS_TRANSFER", "120 distance units; 3 tick uplink"),
                        new OptionInfo("ASTEROID_BELT", "160 distance units; high debris")),
                List.of(new OptionInfo("EXPLORER", "Balanced, fuel 100, crew 3"),
                        new OptionInfo("FREIGHTER", "Large fuel tank, crew 2"),
                        new OptionInfo("CREW_CAPSULE", "Strong life support, crew 4")),
                List.of(new OptionInfo("EASY", "Fewer failures; larger response window"),
                        new OptionInfo("NORMAL", "Standard operating conditions"),
                        new OptionInfo("HARD", "More faults; shorter response window")));
    }
    @Override public void configureMission(MissionConfig config) { engine.configure(config); }
    @Override public void loadScenario(int scenarioIndex) { engine.loadScenario(scenarioIndex); }
    @Override public void start() { engine.start(); }
    @Override public void pause() { engine.pause(); }
    @Override public void tick() { engine.tick(); }
    @Override public boolean isRunning() { return engine.running(); }
    @Override public void reset() { engine.reset(); }
    @Override public void setSpeed(int ticksPerSecond) { engine.setSpeed(ticksPerSecond); }
    @Override public void setThrottle(int percent) { engine.setThrottle(percent); }
    @Override public void setLimit(LimitType type, double value, boolean override) { engine.setLimit(type, value, override); }
    @Override public void setAutonomyMode(AutonomyMode mode) { engine.setAutonomyMode(mode); }
    @Override public void executeRecommendation(String recommendationId) { engine.executeRecommendation(recommendationId); }
    @Override public void dismissRecommendation(String recommendationId) { engine.dismissRecommendation(recommendationId); }
    @Override public void assignCrewTask(String crewName, CrewTaskType task) { engine.assignCrewTask(crewName, task); }
    @Override public void setSelfHealing(boolean enabled) { engine.setSelfHealing(enabled); }
    @Override public void setUplinkDelay(boolean enabled) { engine.setUplinkDelay(enabled); }
    @Override public void injectFault(String target, FaultType type) { engine.injectFault(target, type); }
    @Override public PreviewResult previewRecommendation(String recommendationId, int lookaheadTicks) {
        return engine.previewRecommendation(recommendationId, lookaheadTicks);
    }
    @Override public List<SimulationSnapshot> getHistory() { return engine.getHistory(); }
    @Override public void seekTo(int tick) { engine.seekTo(tick); }
    @Override public void resumeLive() { engine.resumeLive(); }
    @Override public String executeCommand(String line) { return engine.executeCommand(line); }
    @Override public String generateReportHtml() { return engine.reportHtml(); }
    @Override public String exportTelemetryCsv() { return engine.exportCsv(); }
    @Override public String getSetting(String key, String fallback) { return Settings.getInstance().get(key, fallback); }
    @Override public void setSetting(String key, String value) { Settings.getInstance().put(key, value); }
    @Override public void addTickListener(TickListener listener) { engine.addTickListener(listener); }
    @Override public void addEventListener(MissionEventListener listener) { engine.addEventListener(listener); }
    @Override public SimulationSnapshot snapshot() { return engine.snapshot(); }
    @Override public SimulationController fork() { return new DefaultSimulationController(engine.copy()); }
    public SimulationEngine engine() { return engine; }
}
