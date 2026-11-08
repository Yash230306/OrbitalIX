package com.orbitalix.state;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Spacecraft;

/**
 * The craft has shut down what it can and is waiting for readings to recover.
 * Demonstrates: State pattern, polymorphism.
 */
public class SafeModeState implements SpacecraftState {

    @Override
    public void handle(Anomaly anomaly, Spacecraft spacecraft) {
        // Recovery actions do the work. This mode only changes when health is nominal again.
    }

    @Override
    public String name() {
        return "SAFE MODE";
    }
}
