package com.orbitalix.recovery;

import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.model.HealthStatus;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.model.Subsystem;
import com.orbitalix.model.ThermalSubsystem;
import com.orbitalix.state.SafeModeState;
import com.orbitalix.util.EventLogger;

/**
 * Sheds load and, when the core is hot, starts cooling.
 * Demonstrates: Command pattern, polymorphism.
 */
public class EnterSafeModeAction implements RecoveryAction {

    @Override
    public void execute(Spacecraft spacecraft) throws OrbitalIXException {
        spacecraft.setSafeMode(true);
        spacecraft.setState(new SafeModeState());
        Subsystem thermal = spacecraft.findSubsystem("Thermal");
        if (thermal instanceof ThermalSubsystem thermalSubsystem
                && thermal.getHealth().ordinal() >= HealthStatus.WARNING.ordinal()) {
            thermalSubsystem.beginCooling();
            EventLogger.getInstance().info("Safe mode: core cooling started");
        } else {
            EventLogger.getInstance().info("Safe mode: non-essential loads shed");
        }
    }
}
