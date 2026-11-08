package com.orbitalix.model;

/**
 * How serious an anomaly is.
 * Demonstrates: abstraction (a fixed set of values the rest of the code can rely on).
 */
public enum Severity {
    INFO,
    WARNING,
    CRITICAL;

    public boolean isAtLeast(Severity other) {
        return this.ordinal() >= other.ordinal();
    }
}
