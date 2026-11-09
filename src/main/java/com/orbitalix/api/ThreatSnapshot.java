package com.orbitalix.api;

public record ThreatSnapshot(String name, int ticksToImpact, double intensity, Vec2 position) {}
