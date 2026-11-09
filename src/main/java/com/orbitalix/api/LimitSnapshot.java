package com.orbitalix.api;

public record LimitSnapshot(LimitType type, double value, double designMax, boolean overridden) {}
