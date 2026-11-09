package com.orbitalix.config;

public enum CraftType {
    EXPLORER(100, 100, 3, 3, 1.0, false),
    FREIGHTER(160, 60, 2, 2, 0.8, false),
    CREW_CAPSULE(70, 140, 4, 5, 1.0, true);

    private final double fuel;
    private final double oxygen;
    private final int crew;
    private final int sealant;
    private final double progressFactor;
    private final boolean strongLifeSupport;

    CraftType(double fuel, double oxygen, int crew, int sealant, double progressFactor, boolean strongLifeSupport) {
        this.fuel = fuel;
        this.oxygen = oxygen;
        this.crew = crew;
        this.sealant = sealant;
        this.progressFactor = progressFactor;
        this.strongLifeSupport = strongLifeSupport;
    }

    public double fuel() { return fuel; }
    public double oxygen() { return oxygen; }
    public int crew() { return crew; }
    public int sealant() { return sealant; }
    public double progressFactor() { return progressFactor; }
    public boolean strongLifeSupport() { return strongLifeSupport; }
}
