package com.orbitalix.api;

public record MissionSnapshot(double progressPercent, double distanceLeft, int etaTicks, boolean ended, String outcome) {}
