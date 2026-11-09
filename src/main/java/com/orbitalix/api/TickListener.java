package com.orbitalix.api;

@FunctionalInterface
public interface TickListener {
    void onTick(SimulationSnapshot snapshot);
}
