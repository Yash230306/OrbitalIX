package com.orbitalix.sensor;

import com.orbitalix.exception.InvalidTelemetryException;
import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.exception.SensorFailureException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * An instrument that turns a physical quantity into a number.
 * Subclasses decide what "normal" looks like. Faults (drift, spike, stuck, dropout, ramp)
 * live here so every sensor type behaves the same way under stress.
 * Demonstrates: abstraction, encapsulation, inheritance, polymorphism (measure()).
 */
public abstract class Sensor {

    private final String id;
    private final String unit;
    private final double baseline;
    private final List<Reading> history = new ArrayList<>();
    private final Random random;

    private String subsystemName = "";
    private Sensor backup;
    private boolean usingBackup;
    private boolean failed;

    private double noise = 0.2;
    private double lastValue;
    private double previousValue;

    private double warnHigh = Double.POSITIVE_INFINITY;
    private double critHigh = Double.POSITIVE_INFINITY;
    private double warnLow = Double.NEGATIVE_INFINITY;
    private double critLow = Double.NEGATIVE_INFINITY;
    private double rateLimit = Double.POSITIVE_INFINITY;

    private double driftPerTick;
    private double accumulatedDrift;
    private double rampPerTick;
    private double rampOffset;
    private boolean rampHasFloor;
    private double rampFloorOffset;
    private Double stuckValue;
    private double spike;
    private boolean dropout;
    private boolean corrupt;
    private boolean cooling;

    protected Sensor(String id, String unit) {
        this(id, unit, 0);
    }

    protected Sensor(String id, String unit, double baseline) {
        this.id = id;
        this.unit = unit;
        this.baseline = baseline;
        this.lastValue = baseline;
        this.previousValue = baseline;
        this.random = new Random(id.hashCode());
    }

    /**
     * The latest physical value, including noise, backup switching and any active fault.
     */
    public abstract double measure();

    /**
     * Measures, stores the value, and refuses bad data with a checked exception.
     */
    public Reading read() throws OrbitalIXException {
        double value = measure();
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            remember(0);
            throw new InvalidTelemetryException(id + " reported invalid telemetry");
        }
        if (failed) {
            remember(value);
            throw new SensorFailureException(id + " has failed");
        }
        return remember(value);
    }

    protected boolean hasActiveBackup() {
        return usingBackup && backup != null;
    }

    protected double noiseSample() {
        return (random.nextDouble() - 0.5) * 2.0 * noise;
    }

    /**
     * Turns a clean sample into the value operators actually see.
     * Backup delegation is handled by subclasses before they call this.
     */
    protected double applyFaultModel(double raw) {
        if (dropout) {
            return corrupt ? Double.NaN : 0;
        }
        if (stuckValue != null) {
            return stuckValue;
        }
        accumulatedDrift += driftPerTick;
        if (cooling) {
            if (rampOffset > 0) {
                rampOffset = Math.max(0, rampOffset - 2.8);
            } else if (rampOffset < 0) {
                rampOffset = Math.min(0, rampOffset + 2.8);
            }
            if (Math.abs(rampOffset) < 0.08 && accumulatedDrift == 0) {
                cooling = false;
            }
        } else if (rampPerTick != 0) {
            rampOffset += rampPerTick;
            if (rampHasFloor && rampPerTick < 0 && rampOffset < rampFloorOffset) {
                rampOffset = rampFloorOffset;
                rampPerTick = 0;
            }
            if (rampHasFloor && rampPerTick > 0 && rampOffset > rampFloorOffset) {
                rampOffset = rampFloorOffset;
                rampPerTick = 0;
            }
        }
        double value = raw + accumulatedDrift + rampOffset;
        if (spike != 0) {
            value += spike;
            spike = 0;
        }
        return value;
    }

    public void applyDrift(double perTick) {
        this.driftPerTick = perTick;
        this.stuckValue = null;
        this.dropout = false;
    }

    public void applyRamp(double perTick) {
        this.rampPerTick = perTick;
        this.rampHasFloor = false;
        this.cooling = false;
        this.stuckValue = null;
        this.dropout = false;
    }

    /** Moves the reading toward an absolute floor or ceiling, then holds it there. */
    public void applyRampToward(double perTick, double target) {
        this.rampPerTick = perTick;
        this.rampFloorOffset = target - baseline;
        this.rampHasFloor = true;
        this.cooling = false;
        this.stuckValue = null;
        this.dropout = false;
    }

    public void applySpike(double magnitude) {
        this.spike = magnitude;
        this.dropout = false;
    }

    /**
     * Freezes the sensor. If it is currently healthy, pin it on the warning line
     * so the fault is visible on the dashboard.
     */
    public void applyStuck() {
        double pin = lastValue;
        if (!isBeyondWarning()) {
            if (warnHigh < Double.POSITIVE_INFINITY / 2) {
                pin = warnHigh;
            } else if (warnLow > Double.NEGATIVE_INFINITY / 2) {
                pin = warnLow;
            }
        }
        this.stuckValue = pin;
        this.dropout = false;
    }

    /** Signal loss that reads as zero (a quiet radio). */
    public void applyDropout() {
        this.dropout = true;
        this.corrupt = false;
        this.stuckValue = null;
    }

    /** Bus corruption that is not a number. {@link #read()} turns that into an exception. */
    public void applyCorruptDropout() {
        this.dropout = true;
        this.corrupt = true;
        this.stuckValue = null;
    }

    public void beginCooling() {
        this.rampPerTick = 0;
        this.driftPerTick = 0;
        this.accumulatedDrift = 0;
        this.stuckValue = null;
        this.dropout = false;
        this.corrupt = false;
        this.cooling = true;
    }

    public void clearFaults() {
        driftPerTick = 0;
        accumulatedDrift = 0;
        rampPerTick = 0;
        rampOffset = 0;
        rampHasFloor = false;
        stuckValue = null;
        spike = 0;
        dropout = false;
        corrupt = false;
        cooling = false;
        failed = false;
        usingBackup = false;
    }

    public void activateBackup() {
        if (backup == null) {
            failed = true;
            return;
        }
        clearFaults();
        usingBackup = true;
        backup.clearFaults();
    }

    public void failPermanently() {
        this.failed = true;
    }

    private Reading remember(double value) {
        previousValue = lastValue;
        lastValue = value;
        Reading reading = new Reading(value, System.currentTimeMillis());
        history.add(reading);
        if (history.size() > 120) {
            history.remove(0);
        }
        return reading;
    }

    public boolean isBeyondWarning() {
        return lastValue >= warnHigh || lastValue <= warnLow;
    }

    public boolean isBeyondCritical() {
        return lastValue >= critHigh || lastValue <= critLow;
    }

    public String getId() {
        return id;
    }

    public String getUnit() {
        return unit;
    }

    public double getBaseline() {
        return baseline;
    }

    public double getLastValue() {
        return lastValue;
    }

    public double getPreviousValue() {
        return previousValue;
    }

    public double getRateLimit() {
        return rateLimit;
    }

    public double getWarnHigh() {
        return warnHigh;
    }

    public double getWarnLow() {
        return warnLow;
    }

    public List<Reading> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public void setSubsystemName(String subsystemName) {
        this.subsystemName = subsystemName;
    }

    public Sensor getBackup() {
        return backup;
    }

    public void setBackup(Sensor backup) {
        this.backup = backup;
    }

    public boolean isUsingBackup() {
        return usingBackup;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isDropout() {
        return dropout;
    }

    public boolean isCooling() {
        return cooling;
    }

    protected void setNoise(double noise) {
        this.noise = noise;
    }

    protected void setRateLimit(double rateLimit) {
        this.rateLimit = rateLimit;
    }

    protected void setHighThresholds(double warning, double critical) {
        this.warnHigh = warning;
        this.critHigh = critical;
    }

    protected void setLowThresholds(double warning, double critical) {
        this.warnLow = warning;
        this.critLow = critical;
    }
}
