package com.orbitalix.config;

/** Mission destinations carry distance, communications delay, and environmental pressure. */
public enum Destination {
    MOON(80, 1, 0.1, 1),
    MARS_TRANSFER(120, 3, 0.2, 1),
    ASTEROID_BELT(160, 5, 0.35, 2);

    private final double distance;
    private final int uplinkDelay;
    private final double radiationBase;
    private final double debrisMultiplier;

    Destination(double distance, int uplinkDelay, double radiationBase, double debrisMultiplier) {
        this.distance = distance;
        this.uplinkDelay = uplinkDelay;
        this.radiationBase = radiationBase;
        this.debrisMultiplier = debrisMultiplier;
    }

    public double distance() { return distance; }
    public int uplinkDelay() { return uplinkDelay; }
    public double radiationBase() { return radiationBase; }
    public double debrisMultiplier() { return debrisMultiplier; }
}
