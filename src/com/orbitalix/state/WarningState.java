package com.orbitalix.state;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.model.Spacecraft;

/**
 * Something is off, but the craft is still doing its job.
 * Demonstrates: State pattern, polymorphism.
 */
public class WarningState implements SpacecraftState {

    @Override
    public void handle(Anomaly anomaly, Spacecraft spacecraft) {
        if (anomaly.getSeverity() == Severity.CRITICAL) {
            spacecraft.setState(new CriticalState());
            spacecraft.getState().handle(anomaly, spacecraft);
        }
    }

    @Override
    public String name() {
        return "WARNING";
    }
}
