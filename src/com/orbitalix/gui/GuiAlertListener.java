package com.orbitalix.gui;

import com.orbitalix.alert.AlertListener;
import com.orbitalix.model.Anomaly;

/**
 * Adapter receiving domain anomalies and forwarding them into the GUI alert log.
 * Demonstrates: Observer pattern (AlertListener), Adapter/association.
 */
public class GuiAlertListener implements AlertListener {

    private final AlertLogPanel alertLogPanel;

    public GuiAlertListener(AlertLogPanel alertLogPanel) {
        this.alertLogPanel = alertLogPanel;
    }

    @Override
    public void onAnomaly(Anomaly anomaly) {
        String message = anomaly.getSensor().getId() + " - " + anomaly.getMessage();
        alertLogPanel.appendAlert(anomaly.getSeverity(), message);

        if (anomaly.getSeverity() == com.orbitalix.model.Severity.CRITICAL) {
            SoundEffects.getInstance().playCritical();
        } else if (anomaly.getSeverity() == com.orbitalix.model.Severity.WARNING) {
            SoundEffects.getInstance().playWarning();
        }
    }
}
