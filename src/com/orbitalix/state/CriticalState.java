package com.orbitalix.state;

import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.recovery.EnterSafeModeAction;
import com.orbitalix.util.EventLogger;

/**
 * A limit has been broken. After a few ticks the craft commands itself into safe mode.
 * Demonstrates: State pattern, polymorphism.
 */
public class CriticalState implements SpacecraftState {

    /** Long enough for the red banner to be visible, then recovery takes over. */
    private static final int TICKS_BEFORE_SAFE_MODE = 5;

    private int criticalTicks;

    @Override
    public void handle(Anomaly anomaly, Spacecraft spacecraft) {
        if (anomaly.getSeverity() != Severity.CRITICAL || spacecraft.isSafeMode()) {
            return;
        }
        criticalTicks++;
        if (criticalTicks < TICKS_BEFORE_SAFE_MODE) {
            return;
        }
        try {
            new EnterSafeModeAction().execute(spacecraft);
        } catch (OrbitalIXException ex) {
            EventLogger.getInstance().error(ex);
        }
    }

    @Override
    public String name() {
        return "CRITICAL";
    }
}
