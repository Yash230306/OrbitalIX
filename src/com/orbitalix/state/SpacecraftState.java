package com.orbitalix.state;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Spacecraft;

/**
 * What the spacecraft does with an anomaly depends on the mode it is already in.
 * Demonstrates: State pattern, abstraction.
 */
public interface SpacecraftState {

    void handle(Anomaly anomaly, Spacecraft spacecraft);

    String name();
}
