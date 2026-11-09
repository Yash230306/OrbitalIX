package com.orbitalix.api;

import java.util.List;

/** MVC boundary: the user interface depends on this contract, not the simulation engine. */
public interface SimulationController {
    List<String> getScenarioNames();
    List<String> getFaultTargets();
    MissionConfigOptions getConfigOptions();
    void configureMission(MissionConfig config);
    void loadScenario(int scenarioIndex);
    void start();
    void pause();
    void tick();
    boolean isRunning();
    void reset();
    void setSpeed(int ticksPerSecond);
    void setThrottle(int percent);
    void setLimit(LimitType type, double value, boolean override);
    void setAutonomyMode(AutonomyMode mode);
    void executeRecommendation(String recommendationId);
    void dismissRecommendation(String recommendationId);
    void assignCrewTask(String crewName, CrewTaskType task);
    void setSelfHealing(boolean enabled);
    void setUplinkDelay(boolean enabled);
    void injectFault(String target, FaultType type);
    PreviewResult previewRecommendation(String recommendationId, int lookaheadTicks);
    List<SimulationSnapshot> getHistory();
    void seekTo(int tick);
    void resumeLive();
    String executeCommand(String line);
    String generateReportHtml();
    String exportTelemetryCsv();
    String getSetting(String key, String fallback);
    void setSetting(String key, String value);
    void addTickListener(TickListener listener);
    void addEventListener(MissionEventListener listener);
    SimulationSnapshot snapshot();
    SimulationController fork();
}
