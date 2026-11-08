package com.orbitalix.recovery;

import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.util.EventLogger;

/**
 * Restarts one subsystem a few ticks later, so the failure stays visible first.
 * Demonstrates: Command pattern, polymorphism.
 */
public class RestartSubsystemAction implements RecoveryAction {

    private final String subsystemName;

    public RestartSubsystemAction(String subsystemName) {
        this.subsystemName = subsystemName;
    }

    @Override
    public void execute(Spacecraft spacecraft) throws OrbitalIXException {
        Subsystem subsystem = spacecraft.findSubsystem(subsystemName);
        if (subsystem == null) {
            return;
        }
        subsystem.scheduleRestart(6);
        EventLogger.getInstance().info("Restart scheduled for " + subsystemName);
    }
}
