package com.orbitalix.model;

import com.orbitalix.state.NominalState;
import com.orbitalix.state.SpacecraftState;
import com.orbitalix.util.EventLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The satellite itself. It owns the four subsystems and remembers the operating mode.
 * Demonstrates: composition, encapsulation. The mode object arrives with the state package.
 */
public class Spacecraft {

    private final String name;
    /** Composition: the subsystems are part of this spacecraft. They are not shared. */
    private final List<Subsystem> subsystems;
    private SpacecraftState state = new NominalState();
    private boolean safeMode;
    private boolean unrecoverable;

    public Spacecraft(String name, List<Subsystem> subsystems) {
        this.name = name;
        this.subsystems = new ArrayList<>(subsystems);
    }

    public Spacecraft(List<Subsystem> subsystems) {
        this("OrbitalIX-1", subsystems);
    }

    public void tickMaintenance() {
        for (Subsystem subsystem : subsystems) {
            subsystem.tickMaintenance();
        }
    }

    public void refreshHealth() {
        for (Subsystem subsystem : subsystems) {
            if (!unrecoverable || subsystem.getHealth() != HealthStatus.FAILED) {
                subsystem.evaluateHealth();
            }
        }
    }

    public int countAtOrAbove(HealthStatus level) {
        int count = 0;
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getHealth().ordinal() >= level.ordinal()
                    && subsystem.getHealth() != HealthStatus.FAILED) {
                count++;
            }
            if (subsystem.getHealth() == HealthStatus.FAILED && level == HealthStatus.CRITICAL) {
                count++;
            }
        }
        return count;
    }

    public int countCriticalExcluding(String subsystemName) {
        int count = 0;
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getName().equals(subsystemName)) {
                continue;
            }
            HealthStatus health = subsystem.getHealth();
            if (health == HealthStatus.CRITICAL || health == HealthStatus.FAILED) {
                count++;
            }
        }
        return count;
    }

    public void markUnrecoverable() {
        this.unrecoverable = true;
        this.safeMode = true;
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getHealth() == HealthStatus.CRITICAL
                    || subsystem.getHealth() == HealthStatus.WARNING) {
                subsystem.forceHealth(HealthStatus.FAILED);
            }
        }
    }

    public Subsystem findSubsystem(String subsystemName) {
        for (Subsystem subsystem : subsystems) {
            if (subsystem.getName().equalsIgnoreCase(subsystemName)) {
                return subsystem;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public List<Subsystem> getSubsystems() {
        return Collections.unmodifiableList(subsystems);
    }

    public String getModeName() {
        return state.name();
    }

    public SpacecraftState getState() {
        return state;
    }

    public void setState(SpacecraftState state) {
        if (this.state != null && this.state.name().equals(state.name())) {
            this.state = state;
            return;
        }
        this.state = state;
        EventLogger.getInstance().info("Mode is now " + state.name());
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public boolean isUnrecoverable() {
        return unrecoverable;
    }
}
