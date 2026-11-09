package com.orbitalix.api;

public record DependencySnapshot(String from, String to, HealthStatus fromStatus, boolean propagating) {}
