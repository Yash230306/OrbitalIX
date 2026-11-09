package com.orbitalix.api;

import java.util.List;

public record SimulationSnapshot(int tick, boolean replaying, String spacecraftState, int throttle,
        AutonomyMode autonomyMode, boolean autopilotOnline, boolean selfHealing, MissionConfig config,
        MissionSnapshot mission, List<SubsystemSnapshot> subsystems, List<ResourceSnapshot> resources,
        List<LimitSnapshot> limits, List<RecommendationSnapshot> recommendations, List<ThreatSnapshot> threats,
        OrbitSnapshot orbit, List<CrewSnapshot> crew, ScoreSnapshot score, CommsSnapshot comms,
        List<DependencySnapshot> dependencies) {
    public SimulationSnapshot {
        subsystems = List.copyOf(subsystems);
        resources = List.copyOf(resources);
        limits = List.copyOf(limits);
        recommendations = List.copyOf(recommendations);
        threats = List.copyOf(threats);
        crew = List.copyOf(crew);
        dependencies = List.copyOf(dependencies);
    }
}
