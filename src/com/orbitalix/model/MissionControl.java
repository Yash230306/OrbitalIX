package com.orbitalix.model;

import com.orbitalix.alert.AlertListener;
import com.orbitalix.alert.AlertPublisher;
import com.orbitalix.detection.DetectionEngine;
import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.recovery.RecoveryAction;
import com.orbitalix.recovery.RecoveryPlanner;
import com.orbitalix.sensor.Sensor;
import com.orbitalix.state.NominalState;
import com.orbitalix.util.EventLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ground software that reads the spacecraft, runs detectors and publishes alerts.
 * Demonstrates: aggregation (it watches a spacecraft it does not own).
 */
public class MissionControl {

    /** Aggregation: the spacecraft is passed in and can exist without this object. */
    private final Spacecraft spacecraft;
    private final DetectionEngine detectionEngine;
    private final AlertPublisher alertPublisher = new AlertPublisher();
    private final RecoveryPlanner recoveryPlanner = new RecoveryPlanner();
    private final Map<String, Severity> announced = new HashMap<>();
    private final List<OrbitalIXException> handledErrors = new ArrayList<>();
    private OrbitalIXException lastError;

    public MissionControl(Spacecraft spacecraft) {
        this(spacecraft, DetectionEngine.withDefaults());
    }

    public MissionControl(Spacecraft spacecraft, DetectionEngine detectionEngine) {
        this.spacecraft = spacecraft;
        this.detectionEngine = detectionEngine;
    }

    public void addAlertListener(AlertListener listener) {
        alertPublisher.register(listener);
    }

    public void removeAlertListener(AlertListener listener) {
        alertPublisher.remove(listener);
    }

    /**
     * Read every sensor and update subsystem health.
     */
    public void poll() {
        lastError = null;
        spacecraft.tickMaintenance();
        for (Subsystem subsystem : spacecraft.getSubsystems()) {
            for (Sensor sensor : subsystem.getSensors()) {
                try {
                    sensor.read();
                } catch (OrbitalIXException ex) {
                    remember(ex);
                }
            }
            if (subsystem instanceof CommunicationSubsystem communication) {
                try {
                    communication.checkLink();
                } catch (OrbitalIXException ex) {
                    remember(ex);
                }
            }
        }
        spacecraft.refreshHealth();
    }

    /**
     * One monitoring pass: read, detect, alert, change mode and try to recover.
     */
    public void watch() {
        poll();
        List<Anomaly> anomalies = new ArrayList<>();
        for (Subsystem subsystem : spacecraft.getSubsystems()) {
            for (Sensor sensor : subsystem.getSensors()) {
                Optional<Anomaly> found = detectionEngine.analyze(sensor);
                if (found.isEmpty()) {
                    announced.remove(sensor.getId());
                    continue;
                }
                Anomaly anomaly = found.get();
                anomalies.add(anomaly);
                alignHealth(sensor, anomaly.getSeverity());
                Severity previous = announced.get(sensor.getId());
                announced.put(sensor.getId(), anomaly.getSeverity());
                if (previous == null || anomaly.getSeverity().ordinal() > previous.ordinal()) {
                    alertPublisher.notifyListeners(anomaly);
                }
            }
        }
        Anomaly worst = worstOf(anomalies);
        if (worst != null) {
            spacecraft.getState().handle(worst, spacecraft);
        }
        if (!spacecraft.isUnrecoverable()) {
            for (Anomaly anomaly : anomalies) {
                tryRecover(anomaly);
            }
        }
        if (anomalies.isEmpty()) {
            relaxIfHealthy();
        }
    }

    private void tryRecover(Anomaly anomaly) {
        RecoveryAction action = recoveryPlanner.plan(anomaly, spacecraft);
        if (action == null) {
            return;
        }
        try {
            action.execute(spacecraft);
        } catch (OrbitalIXException ex) {
            remember(ex);
            if (ex instanceof com.orbitalix.exception.UnrecoverableFaultException) {
                spacecraft.markUnrecoverable();
            }
        }
    }

    private void remember(OrbitalIXException ex) {
        lastError = ex;
        handledErrors.add(ex);
        EventLogger.getInstance().error(ex);
    }

    private Anomaly worstOf(List<Anomaly> anomalies) {
        Anomaly worst = null;
        for (Anomaly anomaly : anomalies) {
            if (worst == null || anomaly.getSeverity().ordinal() > worst.getSeverity().ordinal()) {
                worst = anomaly;
            }
        }
        return worst;
    }

    private void relaxIfHealthy() {
        if (spacecraft.isUnrecoverable()) {
            return;
        }
        for (Subsystem subsystem : spacecraft.getSubsystems()) {
            if (subsystem.getHealth() != HealthStatus.NOMINAL) {
                return;
            }
        }
        if (spacecraft.isSafeMode() || !"NOMINAL".equals(spacecraft.getModeName())) {
            spacecraft.setSafeMode(false);
            spacecraft.setState(new NominalState());
        }
    }

    /** The coloured health follows the detector, not only the absolute limit. */
    private void alignHealth(Sensor sensor, Severity severity) {
        Subsystem subsystem = spacecraft.findSubsystem(sensor.getSubsystemName());
        if (subsystem == null || subsystem.getHealth() == HealthStatus.FAILED) {
            return;
        }
        if (severity == Severity.CRITICAL
                && subsystem.getHealth().ordinal() < HealthStatus.CRITICAL.ordinal()) {
            subsystem.forceHealth(HealthStatus.CRITICAL);
        } else if (severity == Severity.WARNING && subsystem.getHealth() == HealthStatus.NOMINAL) {
            subsystem.forceHealth(HealthStatus.WARNING);
        }
    }

    public Spacecraft getSpacecraft() {
        return spacecraft;
    }

    public DetectionEngine getDetectionEngine() {
        return detectionEngine;
    }

    public AlertPublisher getAlertPublisher() {
        return alertPublisher;
    }

    public RecoveryPlanner getRecoveryPlanner() {
        return recoveryPlanner;
    }

    public List<OrbitalIXException> getHandledErrors() {
        return handledErrors;
    }

    public OrbitalIXException getLastError() {
        return lastError;
    }
}
