package com.orbitalix.alert;

import com.orbitalix.model.Anomaly;

/**
 * Anyone who wants to hear about an anomaly implements this.
 * Demonstrates: abstraction, the Observer pattern (listener side).
 */
public interface AlertListener {

    void onAnomaly(Anomaly anomaly);
}
