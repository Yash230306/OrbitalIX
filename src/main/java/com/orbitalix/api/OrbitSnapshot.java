package com.orbitalix.api;

import java.util.List;

public record OrbitSnapshot(Vec2 earth, Vec2 destination, Vec2 position, Vec2 plannedPosition, List<Vec2> plannedPath,
        List<Vec2> trail, List<Vec2> predictedPath, double courseErrorDegrees) {
    public OrbitSnapshot {
        plannedPath = List.copyOf(plannedPath);
        trail = List.copyOf(trail);
        predictedPath = List.copyOf(predictedPath);
    }
}
