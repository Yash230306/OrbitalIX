package com.orbitalix.api;

import java.util.List;

public record PreviewResult(String recommendationId, int lookaheadTicks, String outcome, double fuelAfter,
                            double progressAfter, int ticksToCritical, List<String> sideEffects) {
    public PreviewResult {
        sideEffects = List.copyOf(sideEffects);
    }
}
