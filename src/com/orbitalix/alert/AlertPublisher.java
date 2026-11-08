package com.orbitalix.alert;

import com.orbitalix.model.Anomaly;

import java.util.ArrayList;
import java.util.List;

/**
 * The subject in the Observer pattern. Listeners register here and are told about anomalies.
 * Demonstrates: Observer pattern, aggregation.
 */
public class AlertPublisher {

    /** Aggregation: listeners are registered from outside and outlive a single alert. */
    private final List<AlertListener> listeners = new ArrayList<>();

    public void register(AlertListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void remove(AlertListener listener) {
        listeners.remove(listener);
    }

    public void notifyListeners(Anomaly anomaly) {
        for (AlertListener listener : List.copyOf(listeners)) {
            listener.onAnomaly(anomaly);
        }
    }

    public int getListenerCount() {
        return listeners.size();
    }
}
