package com.orbitalix.recovery;

import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.exception.SensorFailureException;
import com.orbitalix.exception.UnrecoverableFaultException;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.sensor.Sensor;
import com.orbitalix.util.EventLogger;

/**
 * Points a drifting instrument at its spare.
 * Demonstrates: Command pattern, polymorphism, association (holds a sensor reference).
 */
public class SwitchToBackupSensorAction implements RecoveryAction {

    /** Association: the sensor belongs to the spacecraft, not to this action. */
    private final Sensor sensor;

    public SwitchToBackupSensorAction(Sensor sensor) {
        this.sensor = sensor;
    }

    @Override
    public void execute(Spacecraft spacecraft) throws OrbitalIXException {
        if (spacecraft.isSafeMode()
                && spacecraft.countCriticalExcluding(sensor.getSubsystemName()) >= 1) {
            throw new UnrecoverableFaultException(
                    "Backup for " + sensor.getId() + " is unavailable during a cascading failure");
        }
        if (sensor.getBackup() == null) {
            throw new SensorFailureException(sensor.getId() + " has no backup sensor");
        }
        sensor.activateBackup();
        EventLogger.getInstance().info("Switched " + sensor.getId() + " to its backup");
    }
}
