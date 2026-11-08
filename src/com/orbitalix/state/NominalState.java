package com.orbitalix.state;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.model.Spacecraft;

/**
 * Everyday flying. A warning or a critical reading moves the craft to the next mode.
 * Demonstrates: State pattern, polymorphism.
 */
public class NominalState implements SpacecraftState {

    @Override
    public void handle(Anomaly anomaly, Spacecraft spacecraft) {
        if (anomaly.getSeverity() == Severity.CRITICAL) {
            spacecraft.setState(new CriticalState());
            spacecraft.getState().handle(anomaly, spacecraft);
        } else if (anomaly.getSeverity() == Severity.WARNING) {
            spacecraft.setState(new WarningState());
        }
    }

    @Override
    public String name() {
        return "NOMINAL";
    }
}
