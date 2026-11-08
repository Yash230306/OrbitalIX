package com.orbitalix.recovery;

import com.orbitalix.model.Anomaly;
import com.orbitalix.model.Severity;
import com.orbitalix.model.Spacecraft;
import com.orbitalix.sensor.Sensor;

import java.util.HashSet;
import java.util.Set;

/**
 * Chooses one recovery command for an anomaly. Each choice is used only once.
 * Demonstrates: Command pattern. Safe mode itself is issued by CriticalState.
 */
public class RecoveryPlanner {

    private final Set<String> alreadyPlanned = new HashSet<>();

    public RecoveryAction plan(Anomaly anomaly, Spacecraft spacecraft) {
        Sensor sensor = anomaly.getSensor();
        String id = sensor.getId();
        if (id.startsWith("GYRO")
                && "ZScoreDetector".equals(anomaly.getDetectorName())
                && !sensor.isUsingBackup()) {
            return once(id + ":backup", new SwitchToBackupSensorAction(sensor));
        }
        if (id.startsWith("SIG") && anomaly.getSeverity() == Severity.CRITICAL) {
            return once(id + ":restart", new RestartSubsystemAction(sensor.getSubsystemName()));
        }
        if (anomaly.getSeverity() == Severity.CRITICAL && sensor.isFailed()) {
            return once(id + ":restart-failed", new RestartSubsystemAction(sensor.getSubsystemName()));
        }
        return null;
    }

    public void reset() {
        alreadyPlanned.clear();
    }

    private RecoveryAction once(String key, RecoveryAction action) {
        if (!alreadyPlanned.add(key)) {
            return null;
        }
        return action;
    }
}
