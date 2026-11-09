package com.orbitalix.api;

import com.orbitalix.config.CraftType;
import com.orbitalix.config.Destination;
import com.orbitalix.config.Difficulty;

public record MissionConfig(Destination destination, CraftType craftType, Difficulty difficulty, long seed,
                            boolean chaosDirector, int scenarioIndex) {
    public MissionConfig {
        if (destination == null || craftType == null || difficulty == null) {
            throw new IllegalArgumentException("Mission options cannot be null");
        }
        if (scenarioIndex < 0) throw new IllegalArgumentException("Scenario index cannot be negative");
    }

    public static MissionConfig defaults() {
        return new MissionConfig(Destination.MARS_TRANSFER, CraftType.CREW_CAPSULE,
                Difficulty.NORMAL, 42L, false, 0);
    }
}
