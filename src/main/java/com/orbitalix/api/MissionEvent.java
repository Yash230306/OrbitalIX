package com.orbitalix.api;

public record MissionEvent(int tick, EventType type, Severity severity, String message) {}
