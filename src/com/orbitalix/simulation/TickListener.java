package com.orbitalix.simulation;

/**
 * The GUI registers one of these to be told when a tick has finished.
 * Demonstrates: abstraction, the Observer pattern.
 */
public interface TickListener {

    void onTick(SimulationSnapshot snapshot);
}
