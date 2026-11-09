package com.orbitalix.config;

/** Abstract factory for consistent spacecraft resource and crew presets. */
public interface CraftFactory {
    CraftType type();
    default double initialFuel() { return type().fuel(); }
    default double initialOxygen() { return type().oxygen(); }
    default int crewCount() { return type().crew(); }
}
