package com.orbitalix.api;

public record CrewSnapshot(String name, String role, double health, double stress, double radiationDose,
                           String task, int taskTicksLeft, boolean alive) {}
