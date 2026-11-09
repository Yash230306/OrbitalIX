package com.orbitalix.config;

import com.orbitalix.api.MissionConfig;

/** Builder pattern keeps optional mission settings explicit and validates at construction. */
public final class MissionConfigBuilder {
    private Destination destination = Destination.MARS_TRANSFER;
    private CraftType craftType = CraftType.CREW_CAPSULE;
    private Difficulty difficulty = Difficulty.NORMAL;
    private long seed = 42L;
    private boolean chaosDirector;
    private int scenarioIndex;

    public MissionConfigBuilder destination(Destination value) { destination = value; return this; }
    public MissionConfigBuilder craftType(CraftType value) { craftType = value; return this; }
    public MissionConfigBuilder difficulty(Difficulty value) { difficulty = value; return this; }
    public MissionConfigBuilder seed(long value) { seed = value; return this; }
    public MissionConfigBuilder chaosDirector(boolean value) { chaosDirector = value; return this; }
    public MissionConfigBuilder scenarioIndex(int value) { scenarioIndex = value; return this; }
    public MissionConfig build() { return new MissionConfig(destination, craftType, difficulty, seed, chaosDirector, scenarioIndex); }
}
