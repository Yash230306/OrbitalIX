package com.orbitalix.api;

import java.util.List;

public record CommsSnapshot(int delayTicks, double signalStrength, boolean blackout, List<UplinkSnapshot> queue) {
    public CommsSnapshot {
        queue = List.copyOf(queue);
    }
}
