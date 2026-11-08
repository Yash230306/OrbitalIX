package com.orbitalix.simulation;

import java.util.List;

/**
 * Immutable picture of the whole spacecraft after one tick.
 * Demonstrates: encapsulation.
 */
public record SimulationSnapshot(int tick, String spacecraftState, List<SubsystemSnapshot> subsystems) {
}
