package com.orbitalix.alert;

import com.orbitalix.model.Anomaly;

/**
 * Prints an anomaly on the console.
 * Demonstrates: Observer pattern, polymorphism.
 */
public class ConsoleAlertListener implements AlertListener {

    @Override
    public void onAnomaly(Anomaly anomaly) {
        System.out.println("ALERT " + anomaly);
    }
}
